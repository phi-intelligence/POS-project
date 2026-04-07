package com.pos.identity_service.service;

import com.pos.identity_service.config.JwtProperties;
import com.pos.identity_service.model.RefreshToken;
import com.pos.identity_service.model.User;
import com.pos.identity_service.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_REVOKED = "REVOKED";

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public RefreshToken create(User user, String ipAddress, String userAgent) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setStatus(STATUS_ACTIVE);
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenSeconds()));
        token.setIpAddress(ipAddress);
        token.setUserAgent(userAgent);
        return refreshTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findActiveByToken(String token) {
        return refreshTokenRepository.findByTokenAndStatus(token, STATUS_ACTIVE)
                .filter(rt -> rt.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setStatus(STATUS_REVOKED);
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .forEach(t -> {
                    t.setStatus(STATUS_REVOKED);
                    refreshTokenRepository.save(t);
                });
    }
}

