package com.pos.identity_service.repository;

import com.pos.identity_service.model.LoginAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoginAuditLogRepository extends JpaRepository<LoginAuditLog, Long> {
    List<LoginAuditLog> findByUsernameOrderByCreatedAtDesc(String username);
}