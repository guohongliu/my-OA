package com.example.backend.controller;

import com.example.backend.service.AuditService;
import com.example.backend.service.JwtService;
import com.example.backend.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @org.springframework.beans.factory.annotation.Value("${security.auth.max-failed}")
    private int maxFailed;

    @org.springframework.beans.factory.annotation.Value("${security.auth.lock-minutes}")
    private int lockMinutes;

    private final com.example.backend.repository.UserAccountRepository userRepo;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, RefreshTokenService refreshTokenService, AuditService auditService, com.example.backend.repository.UserAccountRepository userRepo) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.userRepo = userRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body, jakarta.servlet.http.HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        var optUser = userRepo.findByUsername(username);
        if (optUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        var uaEntity = optUser.get();
        java.time.Instant now = java.time.Instant.now();
        if (uaEntity.getLockedUntil() != null && now.isBefore(uaEntity.getLockedUntil())) {
            auditService.recordWithContext(username, "LOGIN_LOCKED", "Auth", username, null, request.getRemoteAddr(), request.getHeader("X-Device-Id"), request.getHeader("User-Agent"));
            return ResponseEntity.status(423).build();
        }
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (BadCredentialsException ex) {
            int attempts = uaEntity.getFailedAttempts() + 1;
            uaEntity.setFailedAttempts(attempts);
            if (attempts >= maxFailed) {
                uaEntity.setLockedUntil(now.plus(java.time.Duration.ofMinutes(lockMinutes)));
                uaEntity.setFailedAttempts(0);
                userRepo.save(uaEntity);
                auditService.recordWithContext(username, "LOGIN_LOCKED", "Auth", username, null, request.getRemoteAddr(), request.getHeader("X-Device-Id"), request.getHeader("User-Agent"));
                return ResponseEntity.status(423).build();
            }
            userRepo.save(uaEntity);
            auditService.recordWithContext(username, "LOGIN_FAILED", "Auth", username, null, request.getRemoteAddr(), request.getHeader("X-Device-Id"), request.getHeader("User-Agent"));
            return ResponseEntity.status(401).build();
        }
        UserDetails u = (UserDetails) auth.getPrincipal();
        var roles = u.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        String access = jwtService.createAccessToken(u.getUsername(), Map.of("roles", roles));
        String refresh = refreshTokenService.issue(u.getUsername());
        String ip = request.getRemoteAddr();
        String deviceId = request.getHeader("X-Device-Id");
        String ua = request.getHeader("User-Agent");
        auditService.recordWithContext(u.getUsername(), "LOGIN_SUCCESS", "Auth", u.getUsername(), null, ip, deviceId, ua);
        uaEntity.setFailedAttempts(0);
        uaEntity.setLockedUntil(null);
        userRepo.save(uaEntity);
        Map<String, Object> res = new HashMap<>();
        res.put("accessToken", access);
        res.put("refreshToken", refresh);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String oldToken = body.get("refreshToken");
        String username = refreshTokenService.validate(oldToken);
        if (username == null) return ResponseEntity.status(401).build();
        String access = jwtService.createAccessToken(username, Map.of());
        refreshTokenService.revoke(oldToken);
        String newRefresh = refreshTokenService.issue(username);
        Map<String, Object> res = new HashMap<>();
        res.put("accessToken", access);
        res.put("refreshToken", newRefresh);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Map<String, String> body) {
        String token = body.get("refreshToken");
        if (token != null) refreshTokenService.revoke(token);
        return ResponseEntity.noContent().build();
    }
}
