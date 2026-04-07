package com.pos.identity_service.service;

import com.pos.identity_service.model.RefreshToken;
import com.pos.identity_service.model.RolePermission;
import com.pos.identity_service.model.User;
import com.pos.identity_service.repository.RolePermissionRepository;
import com.pos.identity_service.repository.UserRepository;
import com.pos.identity_service.security.JwtTokenService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RolePermissionRepository rolePermissionRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       RolePermissionRepository rolePermissionRepository,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.rolePermissionRepository = rolePermissionRepository;
        this.refreshTokenService = refreshTokenService;
    }

    public Optional<AuthResult> authenticate(String username, String rawPassword, String ipAddress, String userAgent) {
        return userRepository.findByUsername(username)
                .filter(user -> "ACTIVE".equalsIgnoreCase(user.getStatus()))
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()))
                .map(user -> {
                    String accessToken = generateTokenForUser(user);
                    RefreshToken refreshToken = refreshTokenService.create(user, ipAddress, userAgent);
                    return new AuthResult(accessToken, refreshToken.getToken(), user);
                });
    }

    public String generateTokenForUser(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        if (user.getRole() != null) {
            claims.put("role", user.getRole().getName());
            var permissions = rolePermissionRepository.findByRole(user.getRole()).stream()
                    .map(RolePermission::getPermissionKey)
                    .toList();
            claims.put("permissions", permissions);
        }
        return jwtTokenService.generateAccessToken(user.getUsername(), claims);
    }

    public record AuthResult(String accessToken, String refreshToken, User user) {
    }
}

