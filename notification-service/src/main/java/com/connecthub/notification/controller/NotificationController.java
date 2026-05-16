package com.connecthub.notification.controller;

import com.connecthub.notification.model.Notification;
import com.connecthub.notification.model.NotificationType;
import com.connecthub.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    /** POST /api/notifications/send — Create a new notification (query param form) */
    @PostMapping("/send")
    public ResponseEntity<Notification> sendNotification(
            @RequestParam UUID userId,
            @RequestParam NotificationType type,
            @RequestParam String title,
            @RequestParam String message,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false) UUID fromUserId) {
        Notification notification = service.createNotification(
            userId, type, title, message, roomId, fromUserId);
        return new ResponseEntity<>(notification, HttpStatus.CREATED);
    }

    /** POST /api/notifications/send-internal — Accept JSON body (for inter-service calls from message-service) */
    @PostMapping("/send-internal")
    public ResponseEntity<Notification> sendNotificationInternal(@RequestBody Map<String, Object> body) {
        UUID userId = UUID.fromString((String) body.get("userId"));
        NotificationType type = NotificationType.valueOf((String) body.getOrDefault("type", "MENTION"));
        String title = (String) body.getOrDefault("title", "Notification");
        String message = (String) body.getOrDefault("message", "");
        UUID roomId = body.containsKey("roomId") && body.get("roomId") != null && !body.get("roomId").toString().isEmpty() ? UUID.fromString(String.valueOf(body.get("roomId"))) : null;
        UUID fromUserId = body.containsKey("fromUserId") && body.get("fromUserId") != null && !body.get("fromUserId").toString().isEmpty() ? UUID.fromString(String.valueOf(body.get("fromUserId"))) : null;
        Notification notification = service.createNotification(userId, type, title, message, roomId, fromUserId);
        return new ResponseEntity<>(notification, HttpStatus.CREATED);
    }

    /** GET /api/notifications/my — Get current user's notifications */
    @GetMapping("/my")
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(service.getNotifications(userId));
    }

    /** GET /api/notifications/unread — Get unread notifications */
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnread(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(service.getUnreadNotifications(userId));
    }

    /** GET /api/notifications/unread-count — Get unread count */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        Map<String, Long> r = new HashMap<>();
        r.put("count", service.getUnreadCount(userId));
        return ResponseEntity.ok(r);
    }

    /** PUT /api/notifications/{id}/read — Mark one as read */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable UUID id) {
        service.markAsRead(id);
        Map<String, String> r = new HashMap<>();
        r.put("message", "Notification marked as read");
        return ResponseEntity.ok(r);
    }

    /** PUT /api/notifications/read-all — Mark all as read */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        service.markAllAsRead(userId);
        Map<String, String> r = new HashMap<>();
        r.put("message", "All notifications marked as read");
        return ResponseEntity.ok(r);
    }

    /** POST /api/notifications/fcm-token — Register FCM token */
    @PostMapping("/fcm-token")
    public ResponseEntity<Map<String, String>> registerFcmToken(Authentication auth, @RequestBody Map<String, String> body) {
        UUID userId = UUID.fromString(auth.getName());
        String token = body.get("token");
        if (token != null) {
            service.registerFcmToken(userId, token);
        }
        Map<String, String> r = new HashMap<>();
        r.put("message", "FCM token registered");
        return ResponseEntity.ok(r);
    }

    /** GET /api/notifications/health */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> r = new HashMap<>();
        r.put("status", "Notification Service is running");
        return ResponseEntity.ok(r);
    }
}
