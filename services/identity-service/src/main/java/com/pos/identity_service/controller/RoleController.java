package com.pos.identity_service.controller;

import com.pos.identity_service.dto.PermissionAssignRequest;
import com.pos.identity_service.dto.PermissionDto;
import com.pos.identity_service.dto.RoleCreateRequest;
import com.pos.identity_service.dto.RoleDto;
import com.pos.identity_service.dto.RoleUpdateRequest;
import com.pos.identity_service.service.RoleService;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<RoleDto> createRole(@RequestBody RoleCreateRequest request) {
        RoleDto created = roleService.createRole(request);
        return ResponseEntity.created(URI.create("/roles/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<RoleDto>> listRoles() {
        return ResponseEntity.ok(roleService.listRoles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDto> getRole(@PathVariable Long id) {
        Optional<RoleDto> role = roleService.getRole(id);
        return role.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDto> updateRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        Optional<RoleDto> updated = roleService.updateRole(id, request);
        return updated.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<List<PermissionDto>> listPermissions(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.listPermissionsForRole(id));
    }

    @PostMapping("/{id}/permissions")
    public ResponseEntity<List<PermissionDto>> assignPermission(@PathVariable Long id,
                                                                @RequestBody PermissionAssignRequest request) {
        return ResponseEntity.ok(roleService.assignPermission(id, request));
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Void> removePermission(@PathVariable Long roleId, @PathVariable Long permissionId) {
        roleService.removePermission(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }
}

