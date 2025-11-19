package com.example.backend.service;

import com.example.backend.domain.Role;
import com.example.backend.domain.Permission;
import com.example.backend.domain.UserAccount;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.PermissionRepository;
import com.example.backend.repository.UserAccountRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {
    private final UserAccountRepository userRepo;
    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;

    public UserService(UserAccountRepository userRepo, RoleRepository roleRepo, PermissionRepository permRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.permRepo = permRepo;
        // bootstrap default users if not present
        bootstrapUser("admin", "admin123", "ROLE_ADMIN", encoder);
        bootstrapUser("user", "user123", "ROLE_USER", encoder);
    }

    private void bootstrapUser(String username, String rawPassword, String roleName, PasswordEncoder encoder) {
        userRepo.findByUsername(username).ifPresentOrElse(u -> {}, () -> {
            Role role = roleRepo.findByName(roleName).orElseGet(() -> { Role r = new Role(); r.setName(roleName); return roleRepo.save(r); });
            if (roleName.equals("ROLE_ADMIN")) {
                ensurePerm(role, "EMPLOYEE_READ");
                ensurePerm(role, "EMPLOYEE_CREATE");
                ensurePerm(role, "EMPLOYEE_UPDATE");
                ensurePerm(role, "EMPLOYEE_DELETE");
                ensurePerm(role, "ORG_CREATE");
                ensurePerm(role, "ORG_UPDATE");
                ensurePerm(role, "ORG_DELETE");
            } else {
                ensurePerm(role, "EMPLOYEE_READ");
            }
            UserAccount ua = new UserAccount();
            ua.setUsername(username);
            ua.setPassword(encoder.encode(rawPassword));
            ua.setEnabled(true);
            ua.getRoles().add(role);
            userRepo.save(ua);
        });
    }

    private void ensurePerm(Role role, String name) {
        Permission p = permRepo.findByName(name).orElseGet(() -> { Permission x = new Permission(); x.setName(name); return permRepo.save(x); });
        role.getPermissions().add(p);
        roleRepo.save(role);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount u = userRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("not found"));
        List<GrantedAuthority> auths = u.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName())).collect(Collectors.toList());
        for (Role r : u.getRoles()) {
            for (Permission p : r.getPermissions()) {
                auths.add(new SimpleGrantedAuthority(p.getName()));
            }
        }
        return new User(u.getUsername(), u.getPassword(), u.isEnabled(), true, true, true, auths);
    }
}