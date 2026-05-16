package com.connecthub.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration using STOMP protocol over SockJS.
 *
 * HOW IT WORKS:
 *
 * 1. STOMP Endpoint (/ws):
 *    The client connects via SockJS to ws://host:8085/ws
 *    SockJS provides fallback transports (xhr-polling, etc.) for
 *    browsers that don't support native WebSockets.
 *
 * 2. Application Destination Prefix (/app):
 *    When a client sends a message to /app/chat.send, Spring routes
 *    it to the @MessageMapping("/chat.send") method in our controller.
 *
 * 3. Topic Prefix (/topic, /queue):
 *    - /topic/room/{roomId} → all subscribers in a room receive messages
 *    - /topic/user/{userId} → private notifications for a specific user
 *    - /queue/ → point-to-point messages (e.g., typing indicators)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Simple in-memory broker for /topic and /queue destinations
        // RabbitMQ STOMP relay can be re-enabled once spring-amqp conflict is resolved
        config.enableSimpleBroker("/topic", "/queue");

        // Messages sent from clients with /app prefix are routed
        // to @MessageMapping methods in controllers
        config.setApplicationDestinationPrefixes("/app");

        // User-specific messages: /user/{userId}/queue/...
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the /ws endpoint that clients connect to
        // withSockJS() enables fallback options for older browsers
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setSuppressCors(true);
    }
}
