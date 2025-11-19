package com.example.backend.controller;

import com.example.backend.domain.AuditLog;
import com.example.backend.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) { this.auditService = auditService; }

    @GetMapping
    public List<AuditLog> list() { return auditService.list(); }
}

