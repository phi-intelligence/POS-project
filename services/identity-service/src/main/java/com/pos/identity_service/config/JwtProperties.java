package com.pos.identity_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * HMAC secret used to sign tokens.
     * In non-test environments this should come from a secure source.
     */
    private String secret;

    /**
     * Access token lifetime in seconds.
     */
    private long accessTokenSeconds;

    /**
     * Refresh token lifetime in seconds.
     */
    private long refreshTokenSeconds;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenSeconds() {
        return accessTokenSeconds;
    }

    public void setAccessTokenSeconds(long accessTokenSeconds) {
        this.accessTokenSeconds = accessTokenSeconds;
    }

    public long getRefreshTokenSeconds() {
        return refreshTokenSeconds;
    }

    public void setRefreshTokenSeconds(long refreshTokenSeconds) {
        this.refreshTokenSeconds = refreshTokenSeconds;
    }
}

