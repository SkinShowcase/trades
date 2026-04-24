package com.skinsshowcase.trades;

import com.skinsshowcase.trades.config.JwtProperties;
import com.skinsshowcase.trades.config.SteamIdProtectionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, SteamIdProtectionProperties.class})
public class TradesApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradesApplication.class, args);
    }
}
