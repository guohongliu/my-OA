package com.example.backend.controller;

import com.example.backend.service.JwtService;
import com.example.backend.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(body.get("username"), body.get("password")));
        UserDetails u = (UserDetails) auth.getPrincipal();
        var roles = u.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        String access = jwtService.createAccessToken(u.getUsername(), Map.of("roles", roles));
        String refresh = refreshTokenService.issue(u.getUsername());
        Map<String, Object> res = new HashMap<>();
        res.put("accessToken", access);
        res.put("refreshToken", refresh);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String token = body.get("refreshToken");
        String username = refreshTokenService.validate(token);
        if (username == null) return ResponseEntity.status(401).build();
        String access = jwtService.createAccessToken(username, Map.of());
        Map<String, Object> res = new HashMap<>();
        res.put("accessToken", access);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Map<String, String> body) {
        String token = body.get("refreshToken");
        if (token != null) refreshTokenService.revoke(token);
        return ResponseEntity.noContent().build();
    }
}