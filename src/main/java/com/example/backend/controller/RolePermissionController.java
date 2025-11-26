package com.example.backend.controller;

import com.example.backend.domain.Permission;
import com.example.backend.domain.Role;
import com.example.backend.repository.PermissionRepository;
import com.example.backend.repository.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class RolePermissionController {
    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;

    public RolePermissionController(RoleRepository roleRepo, PermissionRepository permRepo) {
        this.roleRepo = roleRepo;
        this.permRepo = permRepo;
    }

    @GetMapping("/api/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Role> listRoles() { return roleRepo.findAll(); }

    @PostMapping("/api/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Role> createRole(@RequestBody Map<String,String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) return ResponseEntity.badRequest().build();
        Role r = roleRepo.findByName(name).orElseGet(() -> { Role x = new Role(); x.setName(name); return roleRepo.save(x); });
        return ResponseEntity.ok(r);
    }

    @PutMapping("/api/roles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Map<String,String> body) {
        return roleRepo.findById(id).map(r -> {
            String name = body.get("name"); if (name != null && !name.isBlank()) r.setName(name);
            return ResponseEntity.ok(roleRepo.save(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/roles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        if (!roleRepo.existsById(id)) return ResponseEntity.notFound().build();
        roleRepo.deleteById(id); return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Permission> listPermissions() { return permRepo.findAll(); }

    @PostMapping("/api/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Permission> createPermission(@RequestBody Map<String,String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) return ResponseEntity.badRequest().build();
        Permission p = permRepo.findByName(name).orElseGet(() -> { Permission x = new Permission(); x.setName(name); return permRepo.save(x); });
        return ResponseEntity.ok(p);
    }

    @DeleteMapping("/api/permissions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        if (!permRepo.existsById(id)) return ResponseEntity.notFound().build();
        permRepo.deleteById(id); return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/roles/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Permission>> rolePermissions(@PathVariable Long id) {
        return roleRepo.findById(id).map(r -> ResponseEntity.ok(List.copyOf(r.getPermissions()))).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/roles/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Role> assignPermission(@PathVariable Long id, @RequestBody Map<String,String> body) {
        String permName = body.get("permissionName");
        if (permName == null || permName.isBlank()) return ResponseEntity.badRequest().build();
        return roleRepo.findById(id).map(r -> {
            Permission p = permRepo.findByName(permName).orElseGet(() -> { Permission x = new Permission(); x.setName(permName); return permRepo.save(x); });
            if (r.getPermissions().stream().noneMatch(pp -> pp.getId().equals(p.getId()))) r.getPermissions().add(p);
            return ResponseEntity.ok(roleRepo.save(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/roles/{roleId}/permissions/{permId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Role> removePermission(@PathVariable Long roleId, @PathVariable Long permId) {
        return roleRepo.findById(roleId).map(r -> {
            r.getPermissions().removeIf(pp -> pp.getId().equals(permId));
            return ResponseEntity.ok(roleRepo.save(r));
        }).orElse(ResponseEntity.notFound().build());
    }
}

