package com.pos.identity_service.controller;

import com.pos.identity_service.config.BreakGlassProperties;
import com.pos.identity_service.dto.AdminBootstrapRequest;
import com.pos.identity_service.model.Role;
import com.pos.identity_service.model.User;
import com.pos.identity_service.repository.RoleRepository;
import com.pos.identity_service.repository.UserRepository;
import com.pos.identity_service.service.LoginAuditLogService;
import com.pos.identity_service.service.PasswordPolicy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/bootstrap/admin")
@ConditionalOnProperty(name = "bootstrap.breakglass.enabled", havingValue = "true")
public class BreakGlassAdminController {

    private final BreakGlassProperties props;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAuditLogService auditLogService;

    public BreakGlassAdminController(
            BreakGlassProperties props,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            LoginAuditLogService auditLogService) {
        this.props = props;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ResponseEntity<Void> provisionFirstAdmin(
            @RequestHeader(name = "X-Bootstrap-Token", required = false) String bootstrapToken,
            @RequestHeader(name = "X-Forwarded-For", required = false) String xForwardedFor,
            @RequestHeader(name = "User-Agent", required = false) String userAgent,
            @RequestBody AdminBootstrapRequest request) {

        String clientIp = resolveClientIp(xForwardedFor);

        if (!isAllowedIp(clientIp)) {
            auditLogService.logSystemEvent("bootstrap", clientIp, userAgent, "BOOTSTRAP_ADMIN_DENIED_IP", false);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (!constantTimeEquals(props.getToken(), bootstrapToken)) {
            auditLogService.logSystemEvent("bootstrap", clientIp, userAgent, "BOOTSTRAP_ADMIN_DENIED_TOKEN", false);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String username = (request.getUsername() == null) ? "" : request.getUsername().trim();
        if (username.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }

        PasswordPolicy.validateOrThrow(username, request.getPassword());

        Optional<User> anyAdmin = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null)
                .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole().getName()))
                .findFirst();

        if (anyAdmin.isPresent()) {
            auditLogService.logSystemEvent("bootstrap", clientIp, userAgent, "BOOTSTRAP_ADMIN_ALREADY_PROVISIONED", false);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "admin already provisioned");
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ADMIN");
                    role.setDescription("System administrator");
                    return roleRepository.save(role);
                });

        if (userRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setRole(adminRole);
        admin.setAdminUnitId(request.getAdminUnitId());
        admin.setStatus("ACTIVE");
        admin.setCreatedAt(Instant.now());
        userRepository.save(admin);

        auditLogService.logSystemEvent(username, clientIp, userAgent, "BOOTSTRAP_ADMIN_CREATED", true);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private String resolveClientIp(String xForwardedFor) {
        if (props.isTrustForwardedFor() && xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] parts = xForwardedFor.split(",");
            if (parts.length > 0) {
                return parts[0].trim();
            }
        }
        return null;
    }

    private boolean isAllowedIp(String clientIp) {
        if (props.getAllowedIps() == null || props.getAllowedIps().isEmpty()) {
            return true;
        }
        if (clientIp == null || clientIp.isBlank()) {
            return false;
        }
        return props.getAllowedIps().stream().anyMatch(ip -> ip != null && ip.equals(clientIp));
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "bootstrap token is not configured");
        }
        if (actual == null) {
            return false;
        }
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}

