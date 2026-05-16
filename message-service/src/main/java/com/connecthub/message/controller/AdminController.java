package com.connecthub.message.controller;

import com.connecthub.message.dto.response.MessageResponse;
import com.connecthub.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/messages/admin")
@Tag(name = "Admin Messages", description = "Admin Message management endpoints")
public class AdminController {

    private final MessageService messageService;

    @Autowired
    public AdminController(MessageService messageService) {
        this.messageService = messageService;
    }

    @DeleteMapping("/{messageId}")
    @Operation(summary = "Soft-delete a message (Admin only)")
    public ResponseEntity<MessageResponse> deleteMessageAsAdmin(@PathVariable UUID messageId) {
        MessageResponse response = messageService.deleteMessageAsAdmin(messageId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<java.util.Map<String, Object>> getMessageStats() {
        long count = messageService.getTotalMessageCount();
        return ResponseEntity.ok(java.util.Map.of("totalMessages", count));
    }
}
