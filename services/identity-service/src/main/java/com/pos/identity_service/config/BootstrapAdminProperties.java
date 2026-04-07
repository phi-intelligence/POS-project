package com.pos.identity_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bootstrap.admin")
public class BootstrapAdminProperties {

    /**
     * When enabled, the application will ensure an ADMIN role exists and an admin user exists.
     * Intended for first-time provisioning in dev/staging and controlled deployments.
     */
    private boolean enabled = false;

    private String username = "admin";

    /**
     * No safe default. Must be provided via env/secret when enabled.
     */
    private String password;

    private Long adminUnitId;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

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

    public Long getAdminUnitId() {
        return adminUnitId;
    }

    public void setAdminUnitId(Long adminUnitId) {
        this.adminUnitId = adminUnitId;
    }
}

