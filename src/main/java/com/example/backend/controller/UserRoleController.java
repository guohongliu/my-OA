package com.example.backend.controller;

import com.example.backend.domain.Role;
import com.example.backend.domain.UserAccount;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserRoleController {
    private final UserAccountRepository userRepo;
    private final RoleRepository roleRepo;

    public UserRoleController(UserAccountRepository userRepo, RoleRepository roleRepo) {
        this.userRepo = userRepo; this.roleRepo = roleRepo;
    }

    @GetMapping("/{username}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Role>> listUserRoles(@PathVariable String username) {
        return userRepo.findByUsername(username).map(u -> ResponseEntity.ok(List.copyOf(u.getRoles()))).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{username}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAccount> assignRole(@PathVariable String username, @RequestBody Map<String,String> body) {
        String roleName = body.get("roleName");
        if (roleName == null || roleName.isBlank()) return ResponseEntity.badRequest().build();
        UserAccount u = userRepo.findByUsername(username).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        Role r = roleRepo.findByName(roleName).orElseGet(() -> { Role x = new Role(); x.setName(roleName); return roleRepo.save(x); });
        if (u.getRoles().stream().noneMatch(rr -> rr.getId().equals(r.getId()))) u.getRoles().add(r);
        return ResponseEntity.ok(userRepo.save(u));
    }

    @DeleteMapping("/{username}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAccount> removeRole(@PathVariable String username, @PathVariable Long roleId) {
        return userRepo.findByUsername(username).map(u -> {
            u.getRoles().removeIf(rr -> rr.getId().equals(roleId));
            return ResponseEntity.ok(userRepo.save(u));
        }).orElse(ResponseEntity.notFound().build());
    }
}

