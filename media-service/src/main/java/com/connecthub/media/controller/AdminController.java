package com.connecthub.media.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/media/admin")
public class AdminController {

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getMediaStats() {
        // Placeholder for file storage used (in bytes)
        // E.g., 500 MB = 500 * 1024 * 1024
        long simulatedStorageUsed = 524288000L; 
        Map<String, Long> response = new HashMap<>();
        response.put("storageUsedBytes", simulatedStorageUsed);
        return ResponseEntity.ok(response);
    }
}
