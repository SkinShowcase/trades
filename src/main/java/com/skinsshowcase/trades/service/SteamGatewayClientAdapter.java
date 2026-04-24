package com.skinsshowcase.trades.service;

import com.skinsshowcase.trades.dto.InventoryResponseDto;
import com.skinsshowcase.trades.client.SteamGatewayClient;
import com.skinsshowcase.trades.exception.InvalidTradeSelectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Синхронный адаптер к реактивному SteamGatewayClient для вызова из сервиса (block).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SteamGatewayClientAdapter {

    private final SteamGatewayClient steamGatewayClient;

    public InventoryResponseDto getInventoryBlocking(String steamId) {
        return steamGatewayClient.getInventory(steamId)
                .onErrorResume(this::wrapToInvalidTradeSelection)
                .block();
    }

    private Mono<InventoryResponseDto> wrapToInvalidTradeSelection(Throwable e) {
        log.warn("Failed to fetch inventory for validation: {}", e.getMessage());
        return Mono.error(new InvalidTradeSelectionException("Cannot verify inventory: " + e.getMessage(), e));
    }
}
