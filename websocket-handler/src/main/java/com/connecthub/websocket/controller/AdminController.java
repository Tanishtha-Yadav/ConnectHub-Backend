package com.connecthub.websocket.controller;

import com.connecthub.websocket.listener.WebSocketEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ws/admin")
public class AdminController {

    private final WebSocketEventListener webSocketEventListener;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Autowired
    public AdminController(WebSocketEventListener webSocketEventListener, org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.webSocketEventListener = webSocketEventListener;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/connections")
    public ResponseEntity<Map<String, Integer>> getActiveConnections() {
        int count = webSocketEventListener.getActiveConnectionCount();
        Map<String, Integer> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

@PostMapping("/disconnect/{userId}")
    public ResponseEntity<Map<String, String>> disconnectUser(@org.springframework.web.bind.annotation.PathVariable String userId) {
        // 1. Tell the specific user to log out immediately
        Map<String, Object> forceLogoutPayload = new HashMap<>();
        forceLogoutPayload.put("type", "FORCE_LOGOUT");
        messagingTemplate.convertAndSend("/topic/user/" + userId, forceLogoutPayload);

        // 2. Broadcast USER_DELETED to all other connected clients
        //    so they refresh their room lists and remove this user
        Map<String, Object> userDeletedPayload = new HashMap<>();
        userDeletedPayload.put("type", "USER_DELETED");
        userDeletedPayload.put("userId", userId);
        messagingTemplate.convertAndSend("/topic/public", userDeletedPayload);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Disconnect signal sent");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, String>> broadcastMessage(@org.springframework.web.bind.annotation.RequestBody Map<String, String> request) {
        String title = request.get("title");
        String message = request.get("message");
        String type = request.get("type"); // INFO, WARNING, ALERT

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "SYSTEM_BROADCAST");
        payload.put("subType", type != null ? type : "INFO");
        payload.put("title", title != null ? title : "System Announcement");
        payload.put("message", message);
        payload.put("timestamp", java.time.LocalDateTime.now().toString());

        messagingTemplate.convertAndSend("/topic/public", payload);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Broadcast sent successfully");
        return ResponseEntity.ok(response);
    }
}
