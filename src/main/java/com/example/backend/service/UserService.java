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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserAccountRepository userRepo, RoleRepository roleRepo, PermissionRepository permRepo) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.permRepo = permRepo;
        // bootstrap default users if not present
        bootstrapUser("admin", "admin123", "ROLE_ADMIN", encoder);
        bootstrapUser("user", "user123", "ROLE_USER", encoder);
    }

    @org.springframework.transaction.annotation.Transactional
    private void bootstrapUser(String username, String rawPassword, String roleName, PasswordEncoder encoder) {
        userRepo.findByUsername(username).ifPresentOrElse(u -> {}, () -> {
            Role role = roleRepo.findByName(roleName).orElseGet(() -> { Role r = new Role(); r.setName(roleName); return roleRepo.save(r); });
            java.util.List<String> names = roleName.equals("ROLE_ADMIN")
                    ? java.util.List.of("EMPLOYEE_READ","EMPLOYEE_CREATE","EMPLOYEE_UPDATE","EMPLOYEE_DELETE","ORG_CREATE","ORG_UPDATE","ORG_DELETE")
                    : java.util.List.of("EMPLOYEE_READ");
            for (String n : names) {
                Permission p = permRepo.findByName(n).orElseGet(() -> { Permission x = new Permission(); x.setName(n); return permRepo.save(x); });
                if (role.getPermissions().stream().noneMatch(pp -> pp.getName().equals(n))) {
                    role.getPermissions().add(p);
                }
            }
            roleRepo.save(role);
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
        if (role.getPermissions().stream().noneMatch(pp -> pp.getName().equals(name))) {
            role.getPermissions().add(p);
            roleRepo.save(role);
        }
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
