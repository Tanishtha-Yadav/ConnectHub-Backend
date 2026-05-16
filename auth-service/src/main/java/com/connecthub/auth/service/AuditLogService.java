package com.connecthub.auth.service;

import com.connecthub.auth.model.AuditLog;
import java.util.List;
import java.util.UUID;

public interface AuditLogService {
    void logAction(UUID adminId, String adminName, String action, String targetId, String details);
    List<AuditLog> getAllLogs();
}
