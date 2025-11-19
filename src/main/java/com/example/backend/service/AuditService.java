package com.example.backend.service;

import com.example.backend.domain.AuditLog;
import com.example.backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    public void record(String actor, String action, String entityType, String entityId, String details) {
        AuditLog e = new AuditLog();
        e.setActor(actor);
        e.setAction(action);
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        e.setDetails(details);
        e.setTimestamp(Instant.now());
        repo.save(e);
    }

    public void recordWithContext(String actor, String action, String entityType, String entityId, String details, String ip, String deviceId, String userAgent) {
        AuditLog e = new AuditLog();
        e.setActor(actor);
        e.setAction(action);
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        e.setDetails(details);
        e.setIp(ip);
        e.setDeviceId(deviceId);
        e.setUserAgent(userAgent);
        e.setTimestamp(Instant.now());
        repo.save(e);
    }

    public List<AuditLog> list() { return repo.findAll(); }
}

