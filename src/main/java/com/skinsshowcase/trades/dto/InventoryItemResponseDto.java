package com.skinsshowcase.trades.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Элемент инвентаря из ответа steam-gateway (для проверки принадлежности и фильтрации предметов).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemResponseDto {

    private String assetId;
    private String classId;
    private String instanceId;
    private String name;
    private String marketHashName;
    private String type;
    private Integer amount;
    private String iconUrl;

    /** Float (степень износа 0–1). */
    private Double floatValue;
    /** Текстовое описание износа (Factory New, Field-Tested и т.д.). */
    private String wearName;
    /** Pattern / paint seed (индекс паттерна скина из steam-gateway). */
    private Integer pattern;
    /** Название коллекции (из тегов Steam). */
    private String collectionName;

    /**
     * Минимальная цена USD из каталога items (по classId); заполняется при выдаче ленты с фильтрами.
     */
    private BigDecimal catalogMinPriceUsd;
}
