package com.connecthub.notification.service;

import com.connecthub.notification.model.Notification;
import com.connecthub.notification.model.NotificationType;
import com.connecthub.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * OfflineEmailTask — scheduled job that emails users who have unread
 * notifications (messages, mentions, or room invites) and have been
 * offline for at least 30 minutes.
 *
 * Supported types: NEW_MESSAGE, MENTION, ROOM_INVITE
 * Runs every 30 minutes.
 */
@Component
public class OfflineEmailTask {

    private static final Logger log = LoggerFactory.getLogger(OfflineEmailTask.class);

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate;

    @Value("${PRESENCE_SERVICE_URL:http://localhost:8088}")
    private String presenceServiceUrl;

    @Value("${AUTH_SERVICE_URL:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${ROOM_SERVICE_URL:http://localhost:8082}")
    private String roomServiceUrl;

    public OfflineEmailTask(NotificationRepository notificationRepository,
                            NotificationService notificationService) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.restTemplate = new RestTemplate();
    }

    @Scheduled(fixedRate = 1800000) // Runs every 30 minutes
    public void processMissedNotifications() {
        LocalDateTime thirtyMinsAgo = LocalDateTime.now().minusMinutes(30);
        log.debug("[OfflineEmailTask] Scheduler triggered. Checking notifications older than {}", thirtyMinsAgo);

        // Handle NEW_MESSAGE, MENTION, and ROOM_INVITE types
        List<Notification> unreadNotifs = notificationRepository.findAll().stream()
            .filter(n -> !n.isRead() && !n.isEmailed())
            .filter(n -> n.getType() == NotificationType.NEW_MESSAGE
                      || n.getType() == NotificationType.MENTION
                      || n.getType() == NotificationType.ROOM_INVITE)
            .filter(n -> n.getCreatedAt() != null && n.getCreatedAt().isBefore(thirtyMinsAgo))
            .toList();

        log.debug("[OfflineEmailTask] Found {} eligible unread notification(s) to check", unreadNotifs.size());

        for (Notification n : unreadNotifs) {
            log.debug("[OfflineEmailTask] Processing notification {} (type={}) for userId={}",
                      n.getNotificationId(), n.getType(), n.getUserId());
            try {
                // Step 1: Check if user is online — treat any error as "offline" (fail-open)
                boolean userIsOnline = false;
                try {
                    String presenceUrl = String.format("%s/api/presence/isOnline/%s", presenceServiceUrl, n.getUserId());
                    log.debug("[OfflineEmailTask] Calling presence service: {}", presenceUrl);
                    Map presenceResp = restTemplate.getForObject(presenceUrl, Map.class);
                    log.debug("[OfflineEmailTask] Presence response: {}", presenceResp);
                    userIsOnline = presenceResp != null && Boolean.TRUE.equals(presenceResp.get("online"));
                } catch (Exception presenceEx) {
                    log.warn("[OfflineEmailTask] Presence service unreachable for user {}, treating as offline. Error: {}",
                             n.getUserId(), presenceEx.getMessage());
                }

                if (userIsOnline) {
                    log.debug("[OfflineEmailTask] User {} is online — skipping email", n.getUserId());
                    continue;
                }

                // Step 2: Fetch user's email from auth-service via the lightweight internal endpoint.
                // WHY: The full /api/auth/profile/{userId} returns UserResponse with LocalDateTime and
                // enum fields. Deserializing those into a raw Map via RestTemplate can fail silently
                // when Jackson's JSR-310 module is not registered on the RestTemplate's ObjectMapper.
                // The /api/auth/internal/user-info/{userId} endpoint returns ONLY String fields.
                try {
                    String userUrl = String.format("%s/api/auth/internal/user-info/%s", authServiceUrl, n.getUserId());
                    log.debug("[OfflineEmailTask] Fetching user info from: {}", userUrl);
                    Map userResp = restTemplate.getForObject(userUrl, Map.class);
                    log.debug("[OfflineEmailTask] User info response: {}", userResp);

                    if (userResp != null && userResp.containsKey("email") && !((String) userResp.get("email")).isBlank()) {
                        String email = (String) userResp.get("email");
                        String name  = (String) userResp.getOrDefault("fullName", "User");
                        if (name == null || name.isBlank()) name = "User";

                        // Step 3: Build type-specific subject and body
                        String subject = null;
                        String body = null;
                        boolean skipEmail = false;

                        switch (n.getType()) {
                            case NEW_MESSAGE:
                                if (!"New Direct Message".equals(n.getTitle())) {
                                    log.debug("[OfflineEmailTask] Message is not a direct message. Skipping email.");
                                    skipEmail = true;
                                    break;
                                }
                                subject = "New direct message on ConnectHub";
                                body = buildHtmlEmail(name, "You have unread direct messages on ConnectHub.", n.getMessage(), "Reply Now", "http://localhost:4200/chat");
                                break;
                            case MENTION:
                                subject = "You were mentioned on ConnectHub";
                                body = buildHtmlEmail(name, "Someone mentioned you in a message.", n.getMessage(), "View Message", "http://localhost:4200/chat");
                                break;
                            case ROOM_INVITE:
                                subject = "Room invite on ConnectHub";
                                body = buildHtmlEmail(name, "You have been invited to join a chat room.", n.getMessage(), "Accept Invite", "http://localhost:4200/chat");
                                break;
                            default:
                                subject = "New notification on ConnectHub";
                                body = buildHtmlEmail(name, "You have a new notification on ConnectHub.", "", "Check Notification", "http://localhost:4200/chat");
                        }

                        if (!skipEmail) {
                            log.debug("[OfflineEmailTask] Sending {} email to {} ({})", n.getType(), email, name);
                            boolean success = notificationService.sendEmail(email, subject, body);
                            if (success) {
                                log.info("[OfflineEmailTask] Sent offline email ({}) to {} (userId={})",
                                         n.getType(), email, n.getUserId());
                                n.setEmailed(true);
                                notificationRepository.save(n);
                            } else {
                                log.warn("[OfflineEmailTask] Failed to send email for notification {}", n.getNotificationId());
                            }
                        } else {
                            n.setEmailed(true);
                            notificationRepository.save(n);
                        }

                    } else {
                        log.warn("[OfflineEmailTask] No 'email' field in user profile for userId={}", n.getUserId());
                    }
                } catch (Exception innerE) {
                    log.error("[OfflineEmailTask] Failed to fetch profile or send email for userId={}: {}",
                              n.getUserId(), innerE.getMessage());
                }

            } catch (Exception e) {
                log.error("[OfflineEmailTask] Unexpected error for notification {}: {}",
                          n.getNotificationId(), e.getMessage());
            }
        }
    }

    private String buildHtmlEmail(String name, String headline, String quote, String btnText, String link) {
        String quoteHtml = (quote != null && !quote.isBlank()) 
            ? "<div style=\"margin: 20px 0; padding: 15px; background-color: #f8fafc; border-left: 4px solid #6c63ff; border-radius: 4px; font-style: italic; color: #475569;\">\"" + quote + "\"</div>" 
            : "";
            
        return "<!DOCTYPE html>" +
               "<html><head><style>" +
               "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f1f5f9; margin: 0; padding: 40px 20px; color: #334155; }" +
               ".container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }" +
               ".header { background: linear-gradient(135deg, #6c63ff 0%, #4f46e5 100%); padding: 30px 20px; text-align: center; color: white; }" +
               ".header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 1px; }" +
               ".content { padding: 40px 30px; }" +
               ".content h2 { font-size: 18px; color: #1e293b; margin-top: 0; }" +
               ".content p { font-size: 15px; line-height: 1.6; color: #475569; }" +
               ".btn { display: inline-block; margin-top: 20px; padding: 12px 28px; background-color: #6c63ff; color: #ffffff !important; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 15px; transition: background-color 0.2s; }" +
               ".btn:hover { background-color: #5b54d6; }" +
               ".footer { padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; background: #f8fafc; border-top: 1px solid #e2e8f0; }" +
               "</style></head><body>" +
               "<div class=\"container\">" +
               "<div class=\"header\"><h1>ConnectHub</h1></div>" +
               "<div class=\"content\">" +
               "<h2>Hi " + name + ",</h2>" +
               "<p>" + headline + "</p>" +
               quoteHtml +
               "<a href=\"" + link + "\" class=\"btn\">" + btnText + "</a>" +
               "</div>" +
               "<div class=\"footer\">" +
               "<p>You are receiving this email because you have unread notifications on ConnectHub.</p>" +
               "</div></div></body></html>";
    }
}
