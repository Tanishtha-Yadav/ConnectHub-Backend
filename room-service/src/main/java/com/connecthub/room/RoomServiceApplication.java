package com.connecthub.room;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Room/Channel Service — manages chat rooms, DMs, group channels,
 * membership, and roles.
 *
 * WHY @EnableDiscoveryClient?
 *   Registers this service with Eureka so the API Gateway can discover it
 *   via lb://room-service (no hardcoded URLs needed).
 */
@SpringBootApplication
@EnableDiscoveryClient
public class RoomServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoomServiceApplication.class, args);
    }
}
