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
public class ItemsProperties {

    private final String baseUrl;
    private final long connectTimeoutMs;
    private final long readTimeoutMs;

    public ItemsProperties(
            @NotBlank @Value("${items.base-url}") String baseUrl,
            @Positive @Value("${items.connect-timeout-ms}") long connectTimeoutMs,
            @Positive @Value("${items.read-timeout-ms}") long readTimeoutMs) {
        this.baseUrl = baseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public Duration getReadTimeout() {
        return Duration.ofMillis(readTimeoutMs);
    }
}
