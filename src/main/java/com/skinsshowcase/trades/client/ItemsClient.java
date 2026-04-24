package com.skinsshowcase.trades.client;

import com.skinsshowcase.trades.dto.ItemResponseDto;
import com.skinsshowcase.trades.exception.ItemsClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Клиент к сервису items: получение данных о предмете (название, цена) по class_id для обогащения выбора.
 */
@Slf4j
@Component
public class ItemsClient {

    private static final String ITEM_PATH = "/api/v1/items/{itemId}";

    private final WebClient webClient;

    public ItemsClient(@Qualifier("itemsWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Запрашивает данные о предмете по item_id (classid).
     */
    public Mono<ItemResponseDto> getItem(String itemId) {
        return webClient.get()
                .uri(ITEM_PATH, itemId)
                .retrieve()
                .bodyToMono(ItemResponseDto.class)
                .onErrorMap(WebClientResponseException.class, this::toItemsClientException)
                .onErrorMap(org.springframework.web.reactive.function.client.WebClientException.class,
                        this::toItemsClientExceptionFromRequest);
    }

    private ItemsClientException toItemsClientExceptionFromRequest(
            org.springframework.web.reactive.function.client.WebClientException e) {
        var msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            var cause = e.getCause();
            if (cause != null) {
                msg = cause.getClass().getSimpleName() + (cause.getMessage() != null ? ": " + cause.getMessage() : "");
            } else {
                msg = "request failed";
            }
        }
        return new ItemsClientException("Items service request failed: " + msg, e);
    }

    private ItemsClientException toItemsClientException(WebClientResponseException e) {
        if (HttpStatusCode.valueOf(404).equals(e.getStatusCode())) {
            return new ItemsClientException("Item not found: " + e.getResponseBodyAsString(), e);
        }
        return new ItemsClientException(
                "Items service returned " + e.getStatusCode() + ": " + e.getResponseBodyAsString(),
                e
        );
    }
}
