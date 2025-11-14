package com.example.backend.controller;

import com.example.backend.model.OrgUnit;
import com.example.backend.service.AuditService;
import com.example.backend.service.OrgService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orgs")
public class OrgController {
    private final OrgService orgService;
    private final AuditService auditService;

    public OrgController(OrgService orgService, AuditService auditService) {
        this.orgService = orgService;
        this.auditService = auditService;
    }

    @GetMapping
    public List<OrgUnit> list() { return orgService.list(); }

    @PostMapping
    public ResponseEntity<OrgUnit> create(@RequestBody OrgUnit o, @RequestHeader(value = "X-Actor", required = false) String actor) {
        OrgUnit created = orgService.create(o);
        auditService.record(actor, "CREATE", "OrgUnit", String.valueOf(created.getId()), created.getName());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrgUnit> update(@PathVariable Long id, @RequestBody OrgUnit o, @RequestHeader(value = "X-Actor", required = false) String actor) {
        OrgUnit updated = orgService.update(id, o);
        if (updated == null) return ResponseEntity.notFound().build();
        auditService.record(actor, "UPDATE", "OrgUnit", String.valueOf(id), updated.getName());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestHeader(value = "X-Actor", required = false) String actor) {
        boolean ok = orgService.delete(id);
        if (!ok) return ResponseEntity.notFound().build();
        auditService.record(actor, "DELETE", "OrgUnit", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tree")
    public List<OrgUnit> tree() { return orgService.tree(); }
}

