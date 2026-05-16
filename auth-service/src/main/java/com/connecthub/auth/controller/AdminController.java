package com.connecthub.auth.controller;

import com.connecthub.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/admin")
public class AdminController {

    @Autowired
    private AuthService authService;

    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<Map<String, String>> suspendUser(@PathVariable UUID userId) {
        authService.suspendUser(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User suspended successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/users/{userId}/reactivate")
    public ResponseEntity<Map<String, String>> reactivateUser(@PathVariable UUID userId) {
        authService.reactivateUser(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User reactivated successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable UUID userId, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

        try {
            restTemplate.exchange(roomServiceUrl + "/api/rooms/admin/users/" + userId, org.springframework.http.HttpMethod.DELETE, entity, Void.class);
        } catch (Exception e) { e.printStackTrace(); }

        try {
            restTemplate.exchange(websocketHandlerUrl + "/api/ws/admin/disconnect/" + userId, org.springframework.http.HttpMethod.POST, entity, Void.class);
        } catch (Exception e) { e.printStackTrace(); }

        authService.deleteUser(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @PutMapping("/users/{userId}/prime")
    public ResponseEntity<Map<String, String>> togglePrime(@PathVariable UUID userId, @RequestParam boolean isPrime) {
        com.connecthub.auth.model.User user = userRepository.findById(userId).orElseThrow();
        if (isPrime) {
            user.setPrimeExpirationDate(java.time.LocalDateTime.now().plusYears(100)); // Make prime effectively forever
        } else {
            user.setPrimeExpirationDate(null); // Remove prime
        }
        userRepository.save(user);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User prime status updated successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // AUDIT LOGS

    @Autowired
    private com.connecthub.auth.service.AuditLogService auditLogService;

    @Autowired
    private com.connecthub.auth.repository.UserRepository userRepository;

    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${ROOM_SERVICE_URL:http://ROOM-SERVICE}")
    private String roomServiceUrl;

    @org.springframework.beans.factory.annotation.Value("${MESSAGE_SERVICE_URL:http://MESSAGE-SERVICE}")
    private String messageServiceUrl;

    @org.springframework.beans.factory.annotation.Value("${MEDIA_SERVICE_URL:http://MEDIA-SERVICE}")
    private String mediaServiceUrl;

    @org.springframework.beans.factory.annotation.Value("${WEBSOCKET_HANDLER_URL:http://WEBSOCKET-HANDLER}")
    private String websocketHandlerUrl;

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getPlatformAnalytics(HttpServletRequest request) {
        Map<String, Object> stats = new HashMap<>();

        // 1. Total Users
        stats.put("totalUsers", userRepository.count());

        // Extract JWT from the incoming Authorization header so inter-service calls are authenticated
        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

        // 2. Active Rooms + breakdown by type
        try {
            ResponseEntity<Map> roomRes = restTemplate.exchange(roomServiceUrl + "/api/rooms/admin/stats", org.springframework.http.HttpMethod.GET, entity, Map.class);
            if (roomRes.getBody() != null) {
                Map body = roomRes.getBody();
                stats.put("activeRooms",   body.get("activeRooms"));
                stats.put("directRooms",   body.getOrDefault("directRooms",  0));
                stats.put("groupRooms",    body.getOrDefault("groupRooms",   0));
                stats.put("channelRooms",  body.getOrDefault("channelRooms", 0));
            }
        } catch (Exception e) {
            stats.put("activeRooms",  0);
            stats.put("directRooms",  0);
            stats.put("groupRooms",   0);
            stats.put("channelRooms", 0);
        }

        // 3. Messages Per Day (Mocked for now or total messages)
        try {
            ResponseEntity<Map> msgRes = restTemplate.exchange(messageServiceUrl + "/api/messages/admin/stats", org.springframework.http.HttpMethod.GET, entity, Map.class);
            if (msgRes.getBody() != null) {
                stats.put("totalMessages", msgRes.getBody().get("totalMessages"));
                // Rough estimation for messages per day for demo
                Object totalObj = msgRes.getBody().get("totalMessages");
                long total = totalObj instanceof Number ? ((Number) totalObj).longValue() : 0L;
                stats.put("messagesPerDay", total > 0 ? Math.max(1, total / 30) : 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to fetch message stats: " + e.getMessage());
            stats.put("totalMessages", 0);
            stats.put("messagesPerDay", 0);
        }

        // 4. File Storage Used
        try {
            ResponseEntity<Map> mediaRes = restTemplate.exchange(mediaServiceUrl + "/api/media/admin/stats", org.springframework.http.HttpMethod.GET, entity, Map.class);
            if (mediaRes.getBody() != null) {
                stats.put("fileStorageUsedBytes", mediaRes.getBody().get("storageUsedBytes"));
            }
        } catch (Exception e) {
            stats.put("fileStorageUsedBytes", 0);
        }

        // 5. Active WebSocket Connections
        try {
            ResponseEntity<Map> wsRes = restTemplate.exchange(websocketHandlerUrl + "/api/ws/admin/connections", org.springframework.http.HttpMethod.GET, entity, Map.class);
            if (wsRes.getBody() != null) {
                stats.put("activeConnections", wsRes.getBody().get("count"));
            }
        } catch (Exception e) {
            stats.put("activeConnections", 0);
        }

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/audit")
    public ResponseEntity<java.util.List<com.connecthub.auth.model.AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }

    @PostMapping("/audit")
    public ResponseEntity<Map<String, String>> createAuditLog(@org.springframework.web.bind.annotation.RequestBody Map<String, String> request) {
        UUID adminId = UUID.fromString(request.get("adminId"));
        String adminName = request.get("adminName");
        String action = request.get("action");
        String targetId = request.get("targetId");
        String details = request.get("details");
        
        auditLogService.logAction(adminId, adminName, action, targetId, details);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Audit log created");
        return ResponseEntity.ok(response);
    }
}
