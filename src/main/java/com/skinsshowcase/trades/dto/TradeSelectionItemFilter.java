package com.skinsshowcase.trades.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Параметры фильтрации предметов из набора для обмена.
 * Поиск выполняется только среди скинов, которые пользователь добавил в набор для обмена.
 */
@Data
@Builder
@Schema(description = "Фильтры для списка предметов набора для обмена")
public class TradeSelectionItemFilter {

    @Schema(description = "Минимальный float (0–1). По умолчанию 0")
    private Double minFloat;

    @Schema(description = "Максимальный float (0–1). По умолчанию 1")
    private Double maxFloat;

    @Schema(description = "Подстрока в названии скина (name)")
    private String name;

    @Schema(description = "Подстрока в названии коллекции")
    private String collectionName;

    @Schema(description = "Подстрока в названии износа (Factory New, Field-Tested и т.д.)")
    private String wearName;

    @Schema(description = "Номер паттерна (paint seed / pattern)")
    private Integer pattern;

    @Schema(description = "Редкость: подстрока в поле type (consumer, industrial, mil-spec, restricted, classified, covert, contraband, extraordinary)")
    private String type;

    @Schema(description = "Special: STATTRACK, SOUVENIR или NORMAL")
    private SpecialFilter special;

    @Schema(description = "Минимальная цена USD (каталог items, по classId предмета)")
    private BigDecimal minPriceUsd;

    @Schema(description = "Максимальная цена USD (каталог items)")
    private BigDecimal maxPriceUsd;
}
