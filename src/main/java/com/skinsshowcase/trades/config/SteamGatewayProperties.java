package com.skinsshowcase.trades.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@Getter
public class SteamGatewayProperties {

    private final String baseUrl;
    private final long connectTimeoutMs;
    private final long readTimeoutMs;

    public SteamGatewayProperties(
            @NotBlank @Value("${steam-gateway.base-url}") String baseUrl,
            @Positive @Value("${steam-gateway.connect-timeout-ms}") long connectTimeoutMs,
            @Positive @Value("${steam-gateway.read-timeout-ms}") long readTimeoutMs) {
        this.baseUrl = baseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public Duration getReadTimeout() {
        return Duration.ofMillis(readTimeoutMs);
    }
}
