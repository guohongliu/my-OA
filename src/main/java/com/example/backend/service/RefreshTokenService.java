package com.example.backend.service;

import com.example.backend.domain.RefreshToken;
import com.example.backend.domain.UserAccount;
import com.example.backend.repository.RefreshTokenRepository;
import com.example.backend.repository.UserAccountRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final int refreshExpDays;
    private final RefreshTokenRepository repo;
    private final UserAccountRepository userRepo;
    private final StringRedisTemplate redis;

    public RefreshTokenService(@Value("${security.jwt.refresh.exp-days}") int refreshExpDays,
                               RefreshTokenRepository repo,
                               UserAccountRepository userRepo,
                               ObjectProvider<StringRedisTemplate> redisProvider) {
        this.refreshExpDays = refreshExpDays;
        this.repo = repo;
        this.userRepo = userRepo;
        this.redis = redisProvider.getIfAvailable();
    }

    @Transactional
    public String issue(String username) {
        UserAccount user = userRepo.findByUsername(username).orElseThrow();
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString().replace("-", ""));
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plus(refreshExpDays, ChronoUnit.DAYS));
        rt.setRevoked(false);
        repo.save(rt);
        if (redis != null) {
            redis.opsForValue().set("rt:" + rt.getToken(), user.getUsername(), refreshExpDays, java.util.concurrent.TimeUnit.DAYS);
        }
        return rt.getToken();
    }

    @Transactional(readOnly = true)
    public String validate(String token) {
        if (redis != null) {
            String v = redis.opsForValue().get("rt:" + token);
            if (v != null) return v;
        }
        RefreshToken e = repo.findByToken(token).orElse(null);
        if (e == null || e.isRevoked()) return null;
        if (Instant.now().isAfter(e.getExpiresAt())) return null;
        return e.getUser().getUsername();
    }

    @Transactional
    public void revoke(String token) {
        if (redis != null) redis.delete("rt:" + token);
        repo.findByToken(token).ifPresent(e -> { e.setRevoked(true); repo.save(e); });
    }
}
