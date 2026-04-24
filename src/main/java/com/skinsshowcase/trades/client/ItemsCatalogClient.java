package com.skinsshowcase.trades.client;

import com.skinsshowcase.trades.dto.ItemCatalogPriceResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Клиент к сервису items: минимальная цена USD по Steam classid (item_id каталога).
 */
@Slf4j
@Component
public class ItemsCatalogClient {

    private final WebClient webClient;

    public ItemsCatalogClient(@Qualifier("itemsWebClient") WebClient itemsWebClient) {
        this.webClient = itemsWebClient;
    }

    /**
     * @return {@code null}, если предмет не найден, цена не задана или запрос не удался
     */
    public BigDecimal fetchMinPriceUsdBlocking(String classId) {
        var mono = webClient.get()
                .uri("/api/v1/items/{itemId}", classId)
                .exchangeToMono(this::mapPriceResponse)
                .onErrorResume(WebClientResponseException.class, this::logAndEmpty)
                .onErrorResume(org.springframework.web.reactive.function.client.WebClientException.class, this::logTransportAndEmpty);
        return mono.block();
    }

    private Mono<BigDecimal> mapPriceResponse(ClientResponse response) {
        var status = response.statusCode();
        if (status.value() == 404) {
            return response.releaseBody().then(Mono.empty());
        }
        if (status.is2xxSuccessful()) {
            return response.bodyToMono(ItemCatalogPriceResponseDto.class).map(this::extractPriceOrNull);
        }
        log.warn("Items catalog returned status {}, omitting price", status.value());
        return response.releaseBody().then(Mono.empty());
    }

    private BigDecimal extractPriceOrNull(ItemCatalogPriceResponseDto dto) {
        if (dto == null || dto.getMinPriceUsd() == null) {
            return null;
        }
        return dto.getMinPriceUsd();
    }

    private Mono<BigDecimal> logAndEmpty(WebClientResponseException e) {
        log.debug("Items catalog HTTP error for price: {}", e.getStatusCode().value());
        return Mono.empty();
    }

    private Mono<BigDecimal> logTransportAndEmpty(org.springframework.web.reactive.function.client.WebClientException e) {
        log.warn("Items catalog request failed, omitting price: {}", e.getClass().getSimpleName());
        return Mono.empty();
    }
}
