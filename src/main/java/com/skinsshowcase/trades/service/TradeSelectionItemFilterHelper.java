package com.skinsshowcase.trades.service;

import com.skinsshowcase.trades.dto.InventoryItemResponseDto;
import com.skinsshowcase.trades.dto.TradeSelectionItemFilter;

/**
 * Проверка соответствия предмета инвентаря фильтру набора для обмена.
 */
public final class TradeSelectionItemFilterHelper {

    private static final String STATTRAK_MARKER = "StatTrak";
    private static final String SOUVENIR_MARKER = "Souvenir";

    private TradeSelectionItemFilterHelper() {
    }

    public static boolean matches(InventoryItemResponseDto item, TradeSelectionItemFilter filter) {
        if (filter == null) {
            return true;
        }
        if (!matchesFloat(item, filter)) {
            return false;
        }
        if (!matchesName(item, filter)) {
            return false;
        }
        if (!matchesCollectionName(item, filter)) {
            return false;
        }
        if (!matchesWearName(item, filter)) {
            return false;
        }
        if (!matchesPattern(item, filter)) {
            return false;
        }
        if (!matchesType(item, filter)) {
            return false;
        }
        if (!matchesSpecial(item, filter)) {
            return false;
        }
        if (!matchesPrice(item, filter)) {
            return false;
        }
        return true;
    }

    private static boolean matchesFloat(InventoryItemResponseDto item, TradeSelectionItemFilter filter) {
        var min = filter.getMinFloat() != null ? filter.getMinFloat() : 0.0;
        var max = filter.getMaxFloat() != null ? filter.getMaxFloat() : 1.0;
        var f = item.getFloatValue();
        if (f == null) {
            return false;
        }
        return f >= min && f <= max;
    }

    private static boolean matchesName(InventoryItemResponseDto item, TradeSelectionItemFilter filter) {
        var search = filter.getName();
        if (search == null || search.isBlank()) {
            return true;
        }
        var name = item.getName();
        if (name == null) {
            return false;
        }
        return name.toLowerCase().contains(search.trim().toLowerCase());
    }

    private static boolean matchesCollectionName(InventoryItemResponseDto item, TradeSelectionItemFilter filter) {
        var search = filter.getCollectionName();
        if (search == null || search.isBlank()) {
            return true;
        }
        var col = item.getCollectionName();
        if (col == null) {
            return false;
        }
        return col.toLowerCase().contains(search.trim().toLowerCase());
    }

    private static boolean matchesWearName(InventoryItemResponseDto item, TradeSelectionItemFilter filter) {
        var search = filter.getWearName();
        if (search == null || search.isBlank()) {
            return true;
        }
        var wear = item.getWearName();
        if (wear == null) {
            return false;
        }
        return wear.toLowerCase().contains(search.trim().toLowerCase());
    }

    private static boolean matchesPattern(InventoryItemResponseDto item, TradeSelectionItemFilter filter) {
        var search = filter.getPattern();
        if (search == null) {
            return true;
        }
        var p = item.getPattern();
        return search.equals(p);
    }

    /** Редкость (type): поиск по совпадению слова в поле type (consumer, industrial, mil-spec и т.д.). */
    private static boolean matchesType(InventoryItemResponseDto item, TradeSelectionItemFilter filter) {
        var search = filter.getType();
        if (search == null || search.isBlank()) {
            return true;
        }
        var type = item.getType();
        if (type == null) {
            return false;
        }
        var word = search.trim().toLowerCase();
        var typeLower = type.toLowerCase();
        return typeLower.contains(word);
    }

    private static boolean matchesSpecial(InventoryItemResponseDto item, TradeSelectionItemFilter filter) {
        var special = filter.getSpecial();
        if (special == null) {
            return true;
        }
        var name = item.getName();
        if (name == null) {
            name = "";
        }
        return switch (special) {
            case STATTRACK -> name.contains(STATTRAK_MARKER);
            case SOUVENIR -> name.contains(SOUVENIR_MARKER);
            case NORMAL -> !name.contains(STATTRAK_MARKER) && !name.contains(SOUVENIR_MARKER);
        };
    }

    /**
     * Диапазон цены по каталогу (USD). Если границы не заданы — не фильтруем по цене.
     * При заданной границе предмет без цены в каталоге не считается совпадением.
     */
    private static boolean matchesPrice(InventoryItemResponseDto item, TradeSelectionItemFilter filter) {
        var min = filter.getMinPriceUsd();
        var max = filter.getMaxPriceUsd();
        if (min == null && max == null) {
            return true;
        }
        var price = item.getCatalogMinPriceUsd();
        if (price == null) {
            return false;
        }
        if (min != null && price.compareTo(min) < 0) {
            return false;
        }
        if (max != null && price.compareTo(max) > 0) {
            return false;
        }
        return true;
    }
}
