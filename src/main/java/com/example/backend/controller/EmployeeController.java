package com.example.backend.controller;

import com.example.backend.model.Employee;
import com.example.backend.service.AuditService;
import com.example.backend.service.EmployeeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService employeeService;
    private final AuditService auditService;

    public EmployeeController(EmployeeService employeeService, AuditService auditService) {
        this.employeeService = employeeService;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Employee> list() { return employeeService.list(); }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('EMPLOYEE_CREATE') or hasRole('ADMIN')")
    public ResponseEntity<Employee> create(@RequestBody Employee e, @RequestHeader(value = "X-Actor", required = false) String actor) {
        Employee created = employeeService.create(e);
        auditService.record(actor, "CREATE", "Employee", String.valueOf(created.getId()), created.getName());
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> get(@PathVariable Long id) {
        Employee e = employeeService.get(id);
        if (e == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(e);
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('EMPLOYEE_UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<Employee> update(@PathVariable Long id, @RequestBody Employee e, @RequestHeader(value = "X-Actor", required = false) String actor) {
        Employee updated = employeeService.update(id, e);
        if (updated == null) return ResponseEntity.notFound().build();
        auditService.record(actor, "UPDATE", "Employee", String.valueOf(id), updated.getName());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('EMPLOYEE_DELETE') or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestHeader(value = "X-Actor", required = false) String actor) {
        boolean ok = employeeService.delete(id);
        if (!ok) return ResponseEntity.notFound().build();
        auditService.record(actor, "DELETE", "Employee", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importCsv(@RequestPart("file") MultipartFile file, @RequestHeader(value = "X-Actor", required = false) String actor) throws Exception {
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        String[] lines = text.split("\r?\n");
        int count = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] cols = line.split(",");
            if (cols.length < 2) continue;
            Employee e = new Employee();
            e.setName(cols[0].trim());
            e.setEmail(cols[1].trim());
            if (cols.length > 2) try { e.setOrgId(Long.parseLong(cols[2].trim())); } catch (Exception ignored) {}
            employeeService.create(e);
            count++;
        }
        auditService.record(actor, "IMPORT", "Employee", "batch", String.valueOf(count));
        return ResponseEntity.ok("imported:" + count);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public String exportCsv() {
        StringBuilder sb = new StringBuilder();
        for (Employee e : employeeService.list()) {
            sb.append(e.getName() == null ? "" : e.getName()).append(",")
              .append(e.getEmail() == null ? "" : e.getEmail()).append(",")
              .append(e.getOrgId() == null ? "" : e.getOrgId()).append("\n");
        }
        return sb.toString();
    }
}

