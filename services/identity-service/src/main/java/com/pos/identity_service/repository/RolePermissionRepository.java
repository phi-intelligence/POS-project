package com.pos.identity_service.repository;

import com.pos.identity_service.model.Role;
import com.pos.identity_service.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRole(Role role);
}