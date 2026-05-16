package com.connecthub.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "admin_name")
    private String adminName;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public AuditLog() {
    }

    public AuditLog(UUID adminId, String adminName, String action, String targetId, String details) {
        this.adminId = adminId;
        this.adminName = adminName;
        this.action = action;
        this.targetId = targetId;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getAdminId() { return adminId; }
    public String getAdminName() { return adminName; }
    public String getAction() { return action; }
    public String getTargetId() { return targetId; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
