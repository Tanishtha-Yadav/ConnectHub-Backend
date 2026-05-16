package com.connecthub.notification.service;

import com.connecthub.notification.model.Notification;
import com.connecthub.notification.model.NotificationType;
import com.connecthub.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connecthub.notification.dto.NotificationMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.auth.oauth2.GoogleCredentials;
import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.io.FileInputStream;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * NotificationService — creates, stores, and sends notifications.
 *
 * NOTIFICATION FLOW:
 *   1. Another service (e.g., message-service) calls POST /api/notifications/send
 *   2. This service creates a Notification record in the DB
 *   3. If the user is offline, send an email
 *   4. If the user is online, the WebSocket service handles in-app push
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final JavaMailSender mailSender;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public NotificationService(NotificationRepository repository,
                                JavaMailSender mailSender,
                                RedisTemplate<String, Object> redisTemplate,
                                ChannelTopic topic) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
        this.topic = topic;
    }

    @PostConstruct
    public void initFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                // Try to load credentials from file, otherwise initialize with default application credentials (mock)
                try {
                    FileInputStream serviceAccount = new FileInputStream("firebase-adminsdk.json");
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase initialized with local service account.");
                } catch (Exception ex) {
                    log.warn("Could not find firebase-adminsdk.json, initializing Firebase with dummy credentials. Push notifications will be logged but not sent.");
                    // Dummy credentials can't be instantiated easily, so we just skip initialization or let it fail gracefully.
                }
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
        }
    }

    public void registerFcmToken(UUID userId, String token) {
        redisTemplate.opsForValue().set("fcm_token:" + userId, token);
    }

    public void sendPushNotification(UUID userId, String title, String body) {
        String token = (String) redisTemplate.opsForValue().get("fcm_token:" + userId);
        if (token != null) {
            try {
                if (!FirebaseApp.getApps().isEmpty()) {
                    Message message = Message.builder()
                            .putData("title", title)
                            .putData("body", body)
                            .setToken(token)
                            .build();
                    String response = FirebaseMessaging.getInstance().send(message);
                    log.info("Successfully sent FCM message: " + response);
                } else {
                    log.info("Mock FCM Push Notification to {}: Title='{}', Body='{}'", userId, title, body);
                }
            } catch (Exception e) {
                log.error("Error sending FCM message to user {}: {}", userId, e.getMessage());
            }
        } else {
            log.info("No FCM token found for user {}, skipping push notification.", userId);
        }
    }

    /** Create and persist a notification */
    public Notification createNotification(UUID userId, NotificationType type,
                                            String title, String message,
                                            UUID roomId, UUID fromUserId) {
        Notification notification = new Notification(userId, type, title, message);
        notification.setRoomId(roomId);
        notification.setFromUserId(fromUserId);
        Notification saved = repository.save(notification);

        // Try sending an FCM push notification (useful if user is offline or mobile)
        sendPushNotification(userId, title, message);

        // Publish to Redis for real-time WebSocket delivery
        try {
            NotificationMessage notifMsg = new NotificationMessage();
            notifMsg.setType(NotificationMessage.NotificationType.valueOf(type.name()));
            notifMsg.setFromUserId(fromUserId);
            notifMsg.setRoomId(roomId);
            notifMsg.setPreview(message);
            notifMsg.setTimestamp(java.time.LocalDateTime.now());

            Map<String, Object> wrapper = Map.of(
                "targetUserId", userId.toString(),
                "notification", notifMsg,
                "type", "NOTIFICATION"
            );
            redisTemplate.convertAndSend(topic.getTopic(), wrapper);
            log.info("Notification published to Redis for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish notification to Redis: {}", e.getMessage());
        }

        return saved;
    }

    /** Get all notifications for a user */
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Get unread notifications */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(UUID userId) {
        return repository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    /** Get unread count (for badge display) */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return repository.countByUserIdAndIsReadFalse(userId);
    }

    /** Mark a notification as read */
    public void markAsRead(UUID notificationId) {
        repository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            repository.save(n);
        });
    }

    /** Mark all notifications as read for a user */
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = repository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        repository.saveAll(unread);
    }

    /**
     * Send an email notification.
     * WHY try-catch? Email sending can fail (invalid SMTP, network issue).
     * We don't want that to break the notification flow.
     */
    public boolean sendEmail(String toEmail, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true); // true indicates HTML
            helper.setFrom(senderEmail);
            
            mailSender.send(message);
            log.info("HTML Email sent to {}: {}", toEmail, subject);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }
}
