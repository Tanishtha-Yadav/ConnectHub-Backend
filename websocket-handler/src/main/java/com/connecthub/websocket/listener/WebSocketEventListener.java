package com.connecthub.websocket.listener;

import com.connecthub.websocket.dto.ChatMessage;
import com.connecthub.websocket.service.RedisMessagePublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for WebSocket session lifecycle events.
 *
 * WHEN A USER CONNECTS:
 *   SessionConnectedEvent fires → set ONLINE in presence-service
 *   → broadcast PRESENCE_UPDATE to all user's rooms
 *
 * WHEN A USER DISCONNECTS:
 *   SessionDisconnectEvent fires → set OFFLINE in presence-service
 *   → broadcast PRESENCE_UPDATE (isOnline=false, lastSeenAt=now) to all user's rooms
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final RestTemplate restTemplate;
    private final RedisMessagePublisher redisPublisher;
    private final ObjectMapper objectMapper;

    @Value("${room-service.url:http://room-service}")
    private String roomServiceUrl;

    @Value("${presence-service.url:http://localhost:8088}")
    private String presenceServiceUrl;

    // Maps STOMP sessionId → "token|userId" so we still have the token at disconnect time
    private final ConcurrentHashMap<String, String> sessionData = new ConcurrentHashMap<>();

    public WebSocketEventListener(RestTemplate restTemplate,
                                   RedisMessagePublisher redisPublisher,
                                   ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.redisPublisher = redisPublisher;
        this.objectMapper = objectMapper;
    }

    public int getActiveConnectionCount() {
        return sessionData.size();
    }

    // =========================================================================
    // Connect
    // =========================================================================

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth)) return;

        String userId = auth.getName();
        String token  = (String) auth.getCredentials();
        if (token != null) {
            sessionData.put(accessor.getSessionId(), token + "|" + userId);
        }

        try {
            // Mark user online in presence-service
            restTemplate.postForEntity(presenceServiceUrl + "/api/presence/online/" + userId, null, Void.class);
            log.info("WebSocket CONNECTED: userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to set user online in presence-service: {}", e.getMessage());
        }

        // Broadcast PRESENCE_UPDATE (isOnline=true) to all user's rooms
        broadcastPresenceUpdate(userId, token, true, "online", null);
    }

    // =========================================================================
    // Disconnect
    // =========================================================================

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String stored = sessionData.remove(accessor.getSessionId());

        // Fall back to principal if session map missed it
        String userId = null;
        String token  = null;
        if (stored != null) {
            String[] parts = stored.split("\\|", 2);
            token  = parts[0];
            userId = parts[1];
        } else if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth) {
            userId = auth.getName();
            token  = (String) auth.getCredentials();
        }

        if (userId == null) return;

        String lastSeenAt = LocalDateTime.now().toString();

        try {
            // Mark user offline in presence-service
            restTemplate.postForEntity(presenceServiceUrl + "/api/presence/offline/" + userId, null, Void.class);
            log.info("WebSocket DISCONNECTED: userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to set user offline in presence-service: {}", e.getMessage());
        }

        // Broadcast PRESENCE_UPDATE (isOnline=false, lastSeenAt) to all user's rooms
        broadcastPresenceUpdate(userId, token, false, "offline", lastSeenAt);
    }

    // =========================================================================
    // Helper: broadcast a PRESENCE_UPDATE to every room the user belongs to
    // =========================================================================

    private void broadcastPresenceUpdate(String userId, String token,
                                          boolean isOnline, String status, String lastSeenAt) {
        if (token == null) {
            log.warn("No token available for presence broadcast for userId={}", userId);
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Get the list of rooms this user is in from room-service
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                roomServiceUrl + "/api/rooms/my-rooms",
                HttpMethod.GET,
                entity,
                new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> rooms = response.getBody();
            if (rooms == null || rooms.isEmpty()) return;

            for (Map<String, Object> room : rooms) {
                Object roomIdObj = room.get("roomId");
                if (roomIdObj == null) continue;

                ChatMessage presenceMsg = new ChatMessage();
                presenceMsg.setEventType(ChatMessage.EventType.PRESENCE_UPDATE);
                presenceMsg.setRoomId(UUID.fromString(roomIdObj.toString()));
                presenceMsg.setSenderId(UUID.fromString(userId));
                presenceMsg.setIsOnline(isOnline);
                presenceMsg.setStatus(status);
                if (lastSeenAt != null) {
                    presenceMsg.setLastSeenAt(lastSeenAt);
                }

                redisPublisher.publish(presenceMsg);
            }

            log.debug("Broadcast PRESENCE_UPDATE userId={}, online={}, rooms={}", userId, isOnline, rooms.size());

        } catch (Exception e) {
            log.warn("Failed to broadcast presence update for userId={}: {}", userId, e.getMessage());
        }
    }
}
