package com.skinsshowcase.trades.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * HTTP-клиент к auth (internal API приватности).
 */
@Component
@Getter
public class AuthServiceProperties {

    private final String baseUrl;
    private final long connectTimeoutMs;
    private final long readTimeoutMs;
    private final String internalServiceKey;

    public AuthServiceProperties(
            @Value("${auth-service.base-url}") String baseUrl,
            @Value("${auth-service.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${auth-service.read-timeout-ms}") long readTimeoutMs,
            @Value("${auth-service.internal-service-key}") String internalServiceKey) {
        this.baseUrl = baseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.internalServiceKey = internalServiceKey;
    }

    public Duration getReadTimeout() {
        return Duration.ofMillis(readTimeoutMs);
    }

    public Duration getConnectTimeout() {
        return Duration.ofMillis(connectTimeoutMs);
    }
}
