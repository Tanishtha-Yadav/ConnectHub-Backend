package com.connecthub.auth.service.impl;

import com.connecthub.auth.model.AuditLog;
import com.connecthub.auth.repository.AuditLogRepository;
import com.connecthub.auth.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void logAction(UUID adminId, String adminName, String action, String targetId, String details) {
        AuditLog log = new AuditLog(adminId, adminName, action, targetId, details);
        auditLogRepository.save(log);
    }

    @Override
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}
