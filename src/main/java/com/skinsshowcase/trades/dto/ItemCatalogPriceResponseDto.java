package com.skinsshowcase.trades.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Фрагмент ответа GET /api/v1/items/{itemId} (нужны только поля для цены).
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemCatalogPriceResponseDto {

    private BigDecimal minPriceUsd;
}
