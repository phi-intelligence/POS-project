package com.pos.identity_service.controller;

import com.pos.identity_service.dto.AuthRequest;
import com.pos.identity_service.dto.AuthTokensResponse;
import com.pos.identity_service.dto.RefreshTokenRequest;
import com.pos.identity_service.service.AuthService;
import com.pos.identity_service.service.LoginAuditLogService;
import com.pos.identity_service.service.RefreshTokenService;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAuditLogService loginAuditLogService;

    public AuthController(AuthService authService,
                          RefreshTokenService refreshTokenService,
                          LoginAuditLogService loginAuditLogService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.loginAuditLogService = loginAuditLogService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request,
                                   @RequestHeader(name = "X-Forwarded-For", required = false) String xForwardedFor,
                                   @RequestHeader(name = "User-Agent", required = false) String userAgent) {
        String ip = xForwardedFor;
        Optional<AuthService.AuthResult> result = authService.authenticate(request.getUsername(), request.getPassword(), ip, userAgent);
        if (result.isEmpty()) {
            loginAuditLogService.logFailure(request.getUsername(), ip, userAgent, "INVALID_CREDENTIALS_OR_STATUS");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AuthService.AuthResult auth = result.get();
        loginAuditLogService.logSuccess(auth.user(), ip, userAgent);
        return ResponseEntity.ok(new AuthTokensResponse(auth.accessToken(), auth.refreshToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        return refreshTokenService.findActiveByToken(request.getRefreshToken())
                .map(rt -> {
                    String newAccessToken = authService.generateTokenForUser(rt.getUser());
                    return ResponseEntity.ok(new AuthTokensResponse(newAccessToken, rt.getToken()));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        refreshTokenService.findActiveByToken(request.getRefreshToken())
                .ifPresent(refreshTokenService::revoke);
        return ResponseEntity.noContent().build();
    }
}

