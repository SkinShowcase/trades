package com.skinsshowcase.trades.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.steam-id-protection")
public class SteamIdProtectionProperties {

    /**
     * Base64-encoded 256-bit key (32 bytes) for AES-GCM encryption.
     */
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
