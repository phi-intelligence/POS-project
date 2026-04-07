package com.pos.identity_service.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bootstrap.breakglass")
public class BreakGlassProperties {

    /**
     * Disabled by default. Must be explicitly enabled in a controlled environment.
     */
    private boolean enabled = false;

    /**
     * Shared secret presented as X-Bootstrap-Token. Must be provided via env/secret.
     */
    private String token;

    /**
     * Optional allowlist of client IPs. If empty, no IP restriction is applied at app level.
     */
    private List<String> allowedIps = new ArrayList<>();

    /**
     * When true, the first IP from X-Forwarded-For is treated as the client IP.
     */
    private boolean trustForwardedFor = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<String> getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(List<String> allowedIps) {
        this.allowedIps = allowedIps;
    }

    public boolean isTrustForwardedFor() {
        return trustForwardedFor;
    }

    public void setTrustForwardedFor(boolean trustForwardedFor) {
        this.trustForwardedFor = trustForwardedFor;
    }
}

