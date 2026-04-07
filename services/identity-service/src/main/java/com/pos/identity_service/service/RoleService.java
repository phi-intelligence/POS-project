package com.pos.identity_service.service;

import com.pos.identity_service.dto.PermissionAssignRequest;
import com.pos.identity_service.dto.PermissionDto;
import com.pos.identity_service.dto.RoleCreateRequest;
import com.pos.identity_service.dto.RoleDto;
import com.pos.identity_service.dto.RoleUpdateRequest;
import com.pos.identity_service.model.Role;
import com.pos.identity_service.model.RolePermission;
import com.pos.identity_service.repository.RolePermissionRepository;
import com.pos.identity_service.repository.RoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleService(RoleRepository roleRepository, RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Transactional
    public RoleDto createRole(RoleCreateRequest request) {
        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        Role saved = roleRepository.save(role);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RoleDto> listRoles() {
        return roleRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<RoleDto> getRole(Long id) {
        return roleRepository.findById(id).map(this::toDto);
    }

    @Transactional
    public Optional<RoleDto> updateRole(Long id, RoleUpdateRequest request) {
        return roleRepository.findById(id).map(role -> {
            role.setDescription(request.getDescription());
            Role saved = roleRepository.save(role);
            return toDto(saved);
        });
    }

    @Transactional
    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<PermissionDto> listPermissionsForRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        return rolePermissionRepository.findByRole(role).stream()
                .map(this::toPermissionDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PermissionDto> assignPermission(Long roleId, PermissionAssignRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        RolePermission permission = new RolePermission();
        permission.setRole(role);
        permission.setPermissionKey(request.getPermissionKey());
        rolePermissionRepository.save(permission);
        return listPermissionsForRole(roleId);
    }

    @Transactional
    public void removePermission(Long roleId, Long permissionId) {
        RolePermission permission = rolePermissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));
        if (!permission.getRole().getId().equals(roleId)) {
            throw new IllegalArgumentException("Permission does not belong to role");
        }
        rolePermissionRepository.delete(permission);
    }

    private RoleDto toDto(Role role) {
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        return dto;
    }

    private PermissionDto toPermissionDto(RolePermission permission) {
        PermissionDto dto = new PermissionDto();
        dto.setId(permission.getId());
        dto.setPermissionKey(permission.getPermissionKey());
        return dto;
    }
}

