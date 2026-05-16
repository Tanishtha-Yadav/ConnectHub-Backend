package com.connecthub.websocket.service;

import com.connecthub.websocket.dto.ChatMessage;
import com.connecthub.websocket.dto.NotificationMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis Message Subscriber — listens for messages from other server instances.
 *
 * FLOW:
 * 1. Redis publishes a message to 'chat:messages' channel.
 * 2. This subscriber receives it.
 * 3. We broadcast it to the LOCAL WebSocket subscribers via SimpMessagingTemplate.
 *
 * This ensures that if a user is connected to Instance B, they still get the
 * message sent by a user connected to Instance A.
 */
@Service
public class RedisMessageSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public RedisMessageSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            byte[] body = message.getBody();
            
            // Try to see if it's a ChatMessage or a Map wrapper
            // Since we use Jackson, we can peek or try parsing
            String json = new String(body);
            
            if (json.contains("\"type\":\"NOTIFICATION\"")) {
                // Handle Notification
                Map<String, Object> wrapper = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                String targetUserId = (String) wrapper.get("targetUserId");
                NotificationMessage notif = objectMapper.convertValue(wrapper.get("notification"), NotificationMessage.class);
                
                log.debug("Redis Pub/Sub RECEIVED NOTIF: user={}, type={}", targetUserId, notif.getType());
                
                // Broadcast to the specific user's private topic
                messagingTemplate.convertAndSend("/topic/user/" + targetUserId, notif);
                
            } else {
                // Default to ChatMessage
                ChatMessage chatMessage = objectMapper.readValue(body, ChatMessage.class);
                log.debug("Redis Pub/Sub RECEIVED CHAT: room={}, event={}",
                        chatMessage.getRoomId(), chatMessage.getEventType());

                // Broadcast to the room topic
                messagingTemplate.convertAndSend("/topic/room/" + chatMessage.getRoomId(), chatMessage);
            }

        } catch (Exception e) {
            log.error("Failed to handle Redis message: {}", e.getMessage());
        }
    }
}
