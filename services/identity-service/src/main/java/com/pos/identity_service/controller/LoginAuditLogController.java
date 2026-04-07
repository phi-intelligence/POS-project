package com.pos.identity_service.controller;

import com.pos.identity_service.dto.LoginAuditLogDto;
import com.pos.identity_service.service.LoginAuditLogService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/login-audit")
@PreAuthorize("hasRole('ADMIN')")
public class LoginAuditLogController {

    private final LoginAuditLogService service;

    public LoginAuditLogController(LoginAuditLogService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<LoginAuditLogDto>> list(
            @RequestParam(name = "username", required = false) String username) {
        if (username != null && !username.isEmpty()) {
            return ResponseEntity.ok(service.findByUsername(username));
        }
        return ResponseEntity.ok(service.findAll());
    }
}

