package com.connecthub.websocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;

/**
 * WebSocket Service — the real-time heart of ConnectHub.
 *
 * This service handles all WebSocket connections using STOMP over SockJS.
 * It does NOT store messages (that's message-service's job).
 * Instead, it acts as a real-time message BROKER:
 *   1. Client connects via SockJS → /ws
 *   2. Client subscribes to topics like /topic/room/{roomId}
 *   3. When a message is sent to /app/chat.send, this service
 *      broadcasts it to all subscribers of the room's topic
 *
 * Redis is used for:
 *   - Presence tracking (online/offline status)
 *   - Pub/Sub so multiple instances of this service can share messages
 */
@SpringBootApplication
@EnableDiscoveryClient
public class WebSocketServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebSocketServiceApplication.class, args);
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
