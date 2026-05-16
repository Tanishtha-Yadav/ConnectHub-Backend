package com.connecthub.websocket.controller;
import java.security.Principal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.connecthub.websocket.dto.ChatMessage;
import com.connecthub.websocket.dto.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;
import com.connecthub.websocket.service.RedisMessagePublisher;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP message controller — handles all real-time chat events.
 *
 * MESSAGE FLOW:
 *   1. Client sends to /app/chat.send  → handleChatMessage()
 *   2. Server broadcasts to /topic/room/{roomId} → all room subscribers
 *
 * TYPING FLOW:
 *   1. Client sends to /app/chat.typing → handleTyping()
 *   2. Server broadcasts to /topic/room/{roomId} → others see "X is typing..."
 *
 * READ RECEIPT FLOW:
 *   1. Client sends to /app/chat.read → handleReadReceipt()
 *   2. Server broadcasts to /topic/room/{roomId} → sender sees ✓✓
 *
 * REACTION FLOW:
 *   1. Client sends to /app/chat.reaction → handleReaction()
 *   2. Server broadcasts to /topic/room/{roomId} → all see the emoji
 *
 * WHY SimpMessagingTemplate instead of @SendTo?
 *   @SendTo is static — it can't use dynamic room IDs from the payload.
 *   SimpMessagingTemplate.convertAndSend() lets us build the destination
 *   path dynamically: /topic/room/{roomId}.
 */
@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final RedisMessagePublisher redisPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    @Value("${room-service.url:http://localhost:8082}")
    private String roomServiceUrl;

    @Value("${AUTH_SERVICE_URL:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${MESSAGE_SERVICE_URL:http://localhost:8083}")
    private String messageServiceUrl;

    public ChatController(RedisMessagePublisher redisPublisher, 
                        SimpMessagingTemplate messagingTemplate,
                        @Autowired(required = false) RestTemplate restTemplate) {
        this.redisPublisher = redisPublisher;
        this.messagingTemplate = messagingTemplate;
        this.restTemplate = restTemplate;
    }

    // =========================================================================
    // CHAT — Send a message to a room
    // Client sends to: /app/chat.send
    // Server broadcasts to: /topic/room/{roomId}
    // =========================================================================

    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload ChatMessage message, Principal principal) {
        // Ensure the timestamp is set to server time (prevents spoofing)
        message.setTimestamp(LocalDateTime.now());
        message.setEventType(ChatMessage.EventType.CHAT);

        log.debug("CHAT → room={}, sender={}, content={}",
                message.getRoomId(), message.getSenderId(),
                message.getContent() != null ? message.getContent().substring(0,
                    Math.min(50, message.getContent().length())) : "");

        // Extract token for propagation
        String token = null;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            token = (String) ((UsernamePasswordAuthenticationToken) principal).getCredentials();
        }

        // SECURITY: Verify user is a member of the room before allowing message
        verifyRoomMembership(message.getSenderId().toString(), message.getRoomId().toString(), token);

        // Publish to Redis instead of broadcasting locally
        // The RedisSubscriber will handle the local broadcast on all instances
        redisPublisher.publish(message);

        // Detect and handle mentions for real-time notification
        processMentions(message, token);
    }

    private void processMentions(ChatMessage message, String token) {
        if (message.getContent() == null || message.getContent().isEmpty()) {
            return;
        }

        Pattern pattern = Pattern.compile("@([a-zA-Z0-9_]+)");
        Matcher matcher = pattern.matcher(message.getContent());
        
        while (matcher.find()) {
            String username = matcher.group(1);
            try {
                // Fetch user info from auth-service to get UUID
                String userUrl = String.format("%s/api/auth/user-by-username?username=%s", authServiceUrl, username);
                
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                if (token != null) headers.setBearerAuth(token);
                org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
                
                org.springframework.http.ResponseEntity<Map> userResponse = restTemplate.exchange(
                        userUrl, org.springframework.http.HttpMethod.GET, entity, Map.class);
                Map<String, Object> userResp = userResponse.getBody();
                
                if (userResp != null && userResp.containsKey("userId")) {
                    String targetUserId = (String) userResp.get("userId");
                    
                    // Don't notify self
                    if (targetUserId.equals(message.getSenderId().toString())) continue;

                    NotificationMessage notif = new NotificationMessage();
                    notif.setType(NotificationMessage.NotificationType.MENTION);
                    notif.setFromUserId(message.getSenderId());
                    notif.setFromUserName(message.getSenderName());
                    notif.setRoomId(message.getRoomId());
                    notif.setPreview(message.getContent());
                    
                    // Publish notification to Redis (so all instances can push to the specific user)
                    redisPublisher.publishNotification(targetUserId, notif);
                }
            } catch (Exception e) {
                log.error("Failed to process real-time mention for {}: {}", username, e.getMessage());
            }
        }
    }

    // =========================================================================
    // TYPING — Broadcast typing indicator
    // Client sends to: /app/chat.typing
    // Server broadcasts to: /topic/room/{roomId}
    // =========================================================================

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload ChatMessage message) {
        message.setEventType(ChatMessage.EventType.TYPING);
        message.setTimestamp(LocalDateTime.now());

        log.debug("TYPING → room={}, sender={}", message.getRoomId(), message.getSenderId());

        // Publish to Redis
        redisPublisher.publish(message);
    }

    // =========================================================================
    // READ — Broadcast read receipt
    // Client sends to: /app/chat.read
    // Server broadcasts to: /topic/room/{roomId}
    // =========================================================================

    @MessageMapping("/chat.read")
    public void handleReadReceipt(@Payload ChatMessage message, Principal principal) {
        message.setEventType(ChatMessage.EventType.READ);
        message.setTimestamp(LocalDateTime.now());
        message.setDeliveryStatus("READ");

        log.debug("READ → room={}, reader={}, targetMessageId={}", 
            message.getRoomId(), message.getSenderId(), message.getTargetMessageId());

        // Update read status in message-service
        String token = null;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            token = (String) ((UsernamePasswordAuthenticationToken) principal).getCredentials();
        }

        if (message.getTargetMessageId() != null && token != null) {
            try {
                String url = messageServiceUrl + "/api/messages/delivery-status";
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setBearerAuth(token);
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

                Map<String, Object> body = Map.of(
                    "roomId", message.getRoomId().toString(),
                    "recipientId", message.getSenderId().toString(),
                    "status", "READ",
                    "upToMessageId", message.getTargetMessageId()
                );

                org.springframework.http.HttpEntity<Map<String, Object>> entity = 
                    new org.springframework.http.HttpEntity<>(body, headers);

                restTemplate.exchange(url, org.springframework.http.HttpMethod.PUT, entity, Map.class);
            } catch (Exception e) {
                log.warn("Failed to update read delivery status: {}", e.getMessage());
            }
        }

        // Publish to Redis
        redisPublisher.publish(message);
    }

    // =========================================================================
    // DELIVERED — Broadcast delivery receipt
    // Client sends to: /app/chat.delivered
    // Server broadcasts to: /topic/room/{roomId}
    // =========================================================================

    @MessageMapping("/chat.delivered")
    public void handleDeliveryReceipt(@Payload ChatMessage message, Principal principal) {
        log.debug("DELIVERED → room={}, recipient={}, targetMessageId={}", 
            message.getRoomId(), message.getSenderId(), message.getTargetMessageId());
        message.setEventType(ChatMessage.EventType.DELIVERED);
        message.setTimestamp(LocalDateTime.now());
        message.setDeliveryStatus("DELIVERED");

        String token = null;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            token = (String) ((UsernamePasswordAuthenticationToken) principal).getCredentials();
        }

        if (message.getTargetMessageId() != null && token != null) {
            try {
                String url = messageServiceUrl + "/api/messages/delivery-status";
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setBearerAuth(token);
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

                Map<String, Object> body = Map.of(
                    "roomId", message.getRoomId().toString(),
                    "recipientId", message.getSenderId().toString(),
                    "status", "DELIVERED",
                    "upToMessageId", message.getTargetMessageId()
                );

                org.springframework.http.HttpEntity<Map<String, Object>> entity = 
                    new org.springframework.http.HttpEntity<>(body, headers);

                restTemplate.exchange(url, org.springframework.http.HttpMethod.PUT, entity, Map.class);
            } catch (Exception e) {
                log.warn("Failed to update delivery status: {}", e.getMessage());
            }
        }

        // Publish to Redis
        redisPublisher.publish(message);
    }

    // =========================================================================
    // REACTION — Broadcast emoji reaction
    // Client sends to: /app/chat.reaction
    // Server broadcasts to: /topic/room/{roomId}
    // =========================================================================

    @MessageMapping("/chat.reaction")
    public void handleReaction(@Payload ChatMessage message) {
        message.setEventType(ChatMessage.EventType.REACTION);
        message.setTimestamp(LocalDateTime.now());

        log.debug("REACTION → room={}, sender={}, emoji={}, target={}",
                message.getRoomId(), message.getSenderId(),
                message.getReaction(), message.getTargetMessageId());

        // Publish to Redis
        redisPublisher.publish(message);
    }

    // =========================================================================
    // EDIT — Broadcast message edit
    // Client sends to: /app/chat.edit
    // Server broadcasts to: /topic/room/{roomId}
    // =========================================================================

    @MessageMapping("/chat.edit")
    public void handleEditMessage(@Payload ChatMessage message) {
        message.setEventType(ChatMessage.EventType.EDIT);
        message.setTimestamp(LocalDateTime.now());

        log.debug("EDIT → room={}, messageId={}", message.getRoomId(), message.getMessageId());

        redisPublisher.publish(message);
    }

    // =========================================================================
    // DELETE — Broadcast message delete
    // Client sends to: /app/chat.delete
    // Server broadcasts to: /topic/room/{roomId}
    // =========================================================================

    @MessageMapping("/chat.delete")
    public void handleDeleteMessage(@Payload ChatMessage message) {
        message.setEventType(ChatMessage.EventType.DELETE);
        message.setTimestamp(LocalDateTime.now());

        log.debug("DELETE → room={}, messageId={}", message.getRoomId(), message.getMessageId());

        redisPublisher.publish(message);
    }

    // =========================================================================
    // NOTIFICATION — Send a private notification to a specific user
    // Called internally, not via @MessageMapping
    // =========================================================================

    public void sendNotification(String userId, NotificationMessage notification) {
        messagingTemplate.convertAndSend(
            "/topic/user/" + userId, notification);
    }

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    /**
     * Verify user is a member of the room before allowing message operations.
     * Calls room-service to check membership.
     *
     * NOTE: We log-and-continue (never throw) so that a transient room-service
     * outage does NOT silently swallow STOMP messages. The JWT authentication
     * performed by WebSocketAuthInterceptor is the primary security gate.
     */
    private void verifyRoomMembership(String senderId, String roomId, String token) {
        if (restTemplate == null) {
            log.warn("RestTemplate not configured, skipping room membership check");
            return;
        }
        try {
            String url = String.format("%s/api/rooms/%s/verify-member?userId=%s",
                roomServiceUrl, roomId, senderId);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (token != null) headers.setBearerAuth(token);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            
            org.springframework.http.ResponseEntity<Boolean> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, Boolean.class);
            Boolean isMember = response.getBody();
            
            if (Boolean.FALSE.equals(isMember)) {
                log.warn("Membership check returned false for user={} room={} — message blocked",
                    senderId, roomId);
                throw new SecurityException("You are not a member of this room");
            }
            log.debug("Membership verified: user={} is member of room={}", senderId, roomId);

            // Also check if user is muted
            String muteUrl = String.format("%s/api/rooms/%s/is-muted?userId=%s", roomServiceUrl, roomId, senderId);
            org.springframework.http.ResponseEntity<Boolean> muteResponse = restTemplate.exchange(
                    muteUrl, org.springframework.http.HttpMethod.GET, entity, Boolean.class);
            if (Boolean.TRUE.equals(muteResponse.getBody())) {
                log.warn("Mute check returned true for user={} room={} — message blocked", senderId, roomId);
                throw new SecurityException("You are muted in this room");
            }
        } catch (SecurityException se) {
            // Re-throw explicit membership denial
            throw se;
        } catch (Exception e) {
            // Room-service unreachable / timeout / 403 missing auth — log warning but allow message through.
            // This prevents a room-service blip from silently killing group messaging.
            log.warn("Could not verify membership for user={} room={} (room-service unreachable: {}). " +
                "Allowing message — user is JWT-authenticated.",
                senderId, roomId, e.getMessage());
        }
    }
}
