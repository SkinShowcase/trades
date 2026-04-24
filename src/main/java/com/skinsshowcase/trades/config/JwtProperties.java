package com.skinsshowcase.trades.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.auth.jwt")
@Validated
public class JwtProperties {

    @NotBlank(message = "app.auth.jwt.secret / AUTH_JWT_SECRET must be set")
    private String secret;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
