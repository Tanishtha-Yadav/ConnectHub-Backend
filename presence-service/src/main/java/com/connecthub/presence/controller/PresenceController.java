package com.connecthub.presence.controller;

import com.connecthub.presence.service.PresenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping("/online/{userId}")
    public ResponseEntity<Void> setOnline(@PathVariable String userId) {
        presenceService.setOnline(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/status/{userId}")
    public ResponseEntity<Void> setStatus(@PathVariable String userId, @RequestParam String status) {
        presenceService.setStatus(userId, status);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/offline/{userId}")
    public ResponseEntity<Void> setOffline(@PathVariable String userId) {
        presenceService.setOffline(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/isOnline/{userId}")
    public ResponseEntity<Map<String, Boolean>> isOnline(@PathVariable String userId) {
        boolean online = presenceService.isOnline(userId);
        Map<String, Boolean> res = new HashMap<>();
        res.put("online", online);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{userId}/status")
    public ResponseEntity<Map<String, String>> getStatus(@PathVariable String userId) {
        String status = presenceService.getStatus(userId);
        String lastSeen = presenceService.getLastSeen(userId);
        if ("offline".equalsIgnoreCase(status) || "invisible".equalsIgnoreCase(status)) {
            lastSeen = lastSeen != null ? lastSeen : presenceService.getOrCreateLastSeen(userId);
        }
        
        Map<String, String> res = new HashMap<>();
        res.put("status", status);
        if (lastSeen != null) {
            res.put("lastSeen", lastSeen);
        }
        return ResponseEntity.ok(res);
    }

    @GetMapping("/last-seen/{userId}")
    public ResponseEntity<Map<String, String>> getLastSeen(@PathVariable String userId) {
        String lastSeen = presenceService.getOrCreateLastSeen(userId);
        Map<String, String> res = new HashMap<>();
        if (lastSeen != null) {
            res.put("lastSeen", lastSeen);
        }
        return ResponseEntity.ok(res);
    }

    @GetMapping("/online-users")
    public ResponseEntity<Set<String>> getOnlineUsers() {
        return ResponseEntity.ok(presenceService.getOnlineUsers());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> res = new HashMap<>();
        res.put("status", "Presence Service is running");
        return ResponseEntity.ok(res);
    }
}
