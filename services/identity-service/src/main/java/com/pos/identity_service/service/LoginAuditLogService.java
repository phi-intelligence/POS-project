package com.pos.identity_service.service;

import com.pos.identity_service.dto.LoginAuditLogDto;
import com.pos.identity_service.model.LoginAuditLog;
import com.pos.identity_service.model.User;
import com.pos.identity_service.repository.LoginAuditLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAuditLogService {

    private final LoginAuditLogRepository repository;

    public LoginAuditLogService(LoginAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void logSuccess(User user, String ipAddress, String userAgent) {
        LoginAuditLog log = new LoginAuditLog();
        log.setUserId(user.getId());
        log.setUsername(user.getUsername());
        log.setSuccess(true);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setCreatedAt(Instant.now());
        repository.save(log);
    }

    @Transactional
    public void logFailure(String username, String ipAddress, String userAgent, String reason) {
        LoginAuditLog log = new LoginAuditLog();
        log.setUserId(null);
        log.setUsername(username);
        log.setSuccess(false);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setReason(reason);
        log.setCreatedAt(Instant.now());
        repository.save(log);
    }

    @Transactional
    public void logSystemEvent(String username, String ipAddress, String userAgent, String reason, boolean success) {
        LoginAuditLog log = new LoginAuditLog();
        log.setUserId(null);
        log.setUsername(username);
        log.setSuccess(success);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setReason(reason);
        log.setCreatedAt(Instant.now());
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<LoginAuditLogDto> findAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LoginAuditLogDto> findByUsername(String username) {
        return repository.findByUsernameOrderByCreatedAtDesc(username).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private LoginAuditLogDto toDto(LoginAuditLog log) {
        LoginAuditLogDto dto = new LoginAuditLogDto();
        dto.setId(log.getId());
        dto.setUserId(log.getUserId());
        dto.setUsername(log.getUsername());
        dto.setSuccess(log.isSuccess());
        dto.setIpAddress(log.getIpAddress());
        dto.setUserAgent(log.getUserAgent());
        dto.setReason(log.getReason());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}

