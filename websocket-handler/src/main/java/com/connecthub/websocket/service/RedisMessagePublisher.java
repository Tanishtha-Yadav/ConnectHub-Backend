package com.connecthub.websocket.service;

import com.connecthub.websocket.config.RedisConfig;
import com.connecthub.websocket.dto.ChatMessage;
import com.connecthub.websocket.dto.NotificationMessage;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

/**
 * Redis Message Publisher — sends messages to the Redis channel for synchronization.
 */
@Service
public class RedisMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisMessagePublisher.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic;

    public RedisMessagePublisher(RedisTemplate<String, Object> redisTemplate, ChannelTopic topic) {
        this.redisTemplate = redisTemplate;
        this.topic = topic;
    }

    /**
     * Publish a message to the Redis channel.
     * This will be picked up by all instances of the websocket-service.
     */
    public void publish(ChatMessage message) {
        log.debug("Redis Pub/Sub PUBLISH: room={}, event={}",
                message.getRoomId(), message.getEventType());

        redisTemplate.convertAndSend(topic.getTopic(), message);
    }

    /**
     * Publish a notification for a specific user to Redis.
     */
    public void publishNotification(String userId, NotificationMessage notification) {
        log.debug("Redis Pub/Sub NOTIF: user={}, type={}",
                userId, notification.getType());

        // We wrap it in a map so the subscriber knows it's a notification
        Map<String, Object> wrapper = Map.of(
            "targetUserId", userId,
            "notification", notification,
            "type", "NOTIFICATION"
        );
        
        redisTemplate.convertAndSend(topic.getTopic(), wrapper);
    }
}
