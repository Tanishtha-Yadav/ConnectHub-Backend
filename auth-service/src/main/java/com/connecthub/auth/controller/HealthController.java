package com.connecthub.auth.controller;

import com.connecthub.auth.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * Provides endpoints for monitoring and health checks
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Health check and monitoring endpoints")
public class HealthController {

    /**
     * Health check endpoint
     */
    @GetMapping
    @Operation(summary = "Health check", description = "Returns health status of the service")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("service", "Auth Service");
        healthData.put("version", "1.0.0");
        healthData.put("timestamp", System.currentTimeMillis());
        healthData.put("uptime", "running");

        return ResponseEntity.ok(
                ApiResponse.ok("Service is healthy", healthData)
        );
    }

    /**
     * Ready check endpoint
     */
    @GetMapping("/ready")
    @Operation(summary = "Readiness check", description = "Returns readiness status (Kubernetes liveness probe)")
    public ResponseEntity<ApiResponse<Map<String, String>>> ready() {
        Map<String, String> readyData = new HashMap<>();
        readyData.put("ready", "true");
        readyData.put("service", "Auth Service");

        return ResponseEntity.ok(
                ApiResponse.ok("Service is ready", readyData)
        );
    }

    /**
     * Live check endpoint
     */
    @GetMapping("/live")
    @Operation(summary = "Liveness check", description = "Returns liveness status (Kubernetes readiness probe)")
    public ResponseEntity<ApiResponse<Map<String, String>>> live() {
        Map<String, String> liveData = new HashMap<>();
        liveData.put("alive", "true");
        liveData.put("service", "Auth Service");

        return ResponseEntity.ok(
                ApiResponse.ok("Service is alive", liveData)
        );
    }
}
