package com.example.backend.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final String secret;
    private final int accessExpMinutes;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.access.exp-minutes}") int accessExpMinutes) {
        this.secret = secret;
        this.accessExpMinutes = accessExpMinutes;
    }

    public String createAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessExpMinutes, ChronoUnit.MINUTES)))
                .signWith(Keys.hmacShaKeyFor(keyBytes()))
                .compact();
    }

    public String validateAndGetSubject(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(keyBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    private byte[] keyBytes() {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException e) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
