package com.connecthub.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Notification Service — sends email and push notifications.
 *
 * CHANNELS:
 *   1. Email (via Spring Mail / SMTP)
 *   2. WebSocket push (calls websocket-service to deliver in-app notifications)
 *
 * TRIGGERS:
 *   - New message in a room (for offline users)
 *   - Room invite
 *   - @mention in a message
 *   - System announcements
 */
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
