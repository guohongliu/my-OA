package com.example.backend.service;

import com.example.backend.model.AuditLogEntry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AuditService {
    private final List<AuditLogEntry> logs = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong seq = new AtomicLong(1);

    public void record(String actor, String action, String entityType, String entityId, String details) {
        AuditLogEntry e = new AuditLogEntry();
        e.setId(seq.getAndIncrement());
        e.setActor(actor);
        e.setAction(action);
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        e.setDetails(details);
        e.setTimestamp(Instant.now());
        logs.add(e);
    }

    public List<AuditLogEntry> list() { return new ArrayList<>(logs); }
}

