package com.skinsshowcase.trades.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ответ items GET /api/v1/items/{itemId} — данные о предмете для обогащения выбора.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemResponseDto {

    private String itemId;
    private String name;
    private BigDecimal minPriceUsd;
    private Instant updatedAt;
}
