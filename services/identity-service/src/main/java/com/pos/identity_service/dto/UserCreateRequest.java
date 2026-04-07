package com.pos.identity_service.dto;

public class UserCreateRequest {

    private String username;
    private String password;
    private Long roleId;
    private Long adminUnitId;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getAdminUnitId() {
        return adminUnitId;
    }

    public void setAdminUnitId(Long adminUnitId) {
        this.adminUnitId = adminUnitId;
    }
}

