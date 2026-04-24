package com.skinsshowcase.trades.client;

import com.skinsshowcase.trades.dto.InventoryResponseDto;
import com.skinsshowcase.trades.exception.SteamGatewayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Клиент к steam-gateway: получение инвентаря пользователя для проверки выбранных предметов.
 */
@Slf4j
@Component
public class SteamGatewayClient {

    private static final String INVENTORY_PATH = "/api/v1/inventory/{steamId}";
    private static final int DEFAULT_APP_ID = 730;
    private static final int DEFAULT_CONTEXT_ID = 2;

    private final WebClient webClient;

    public SteamGatewayClient(@Qualifier("steamGatewayWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Запрашивает инвентарь по Steam ID (CS2: appId=730, contextId=2).
     */
    public Mono<InventoryResponseDto> getInventory(String steamId) {
        return getInventory(steamId, DEFAULT_APP_ID, DEFAULT_CONTEXT_ID);
    }

    public Mono<InventoryResponseDto> getInventory(String steamId, int appId, int contextId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path(INVENTORY_PATH)
                        .queryParam("appId", appId)
                        .queryParam("contextId", contextId)
                        .build(steamId))
                .retrieve()
                .bodyToMono(InventoryResponseDto.class)
                .onErrorMap(WebClientResponseException.class, this::toSteamGatewayException)
                .onErrorMap(org.springframework.web.reactive.function.client.WebClientException.class,
                        this::toSteamGatewayExceptionFromRequest);
    }

    private SteamGatewayException toSteamGatewayExceptionFromRequest(
            org.springframework.web.reactive.function.client.WebClientException e) {
        var msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            var cause = e.getCause();
            if (cause != null) {
                var causeMsg = cause.getMessage();
                msg = cause.getClass().getSimpleName()
                        + (causeMsg != null && !causeMsg.isBlank() ? ": " + causeMsg : " (check read/connect timeout)");
            } else {
                msg = "request failed";
            }
        }
        return new SteamGatewayException("Steam gateway request failed: " + msg, e);
    }

    private SteamGatewayException toSteamGatewayException(WebClientResponseException e) {
        return new SteamGatewayException(
                "Steam gateway returned " + e.getStatusCode() + ": " + e.getResponseBodyAsString(),
                e
        );
    }
}
