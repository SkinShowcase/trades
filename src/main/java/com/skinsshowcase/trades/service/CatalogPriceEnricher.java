package com.skinsshowcase.trades.service;

import com.skinsshowcase.trades.client.ItemsCatalogClient;
import com.skinsshowcase.trades.dto.InventoryItemResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Подставляет minPriceUsd из каталога items по classId для списка предметов инвентаря.
 */
@Component
@RequiredArgsConstructor
public class CatalogPriceEnricher {

    private final ItemsCatalogClient itemsCatalogClient;

    public void enrichCatalogPrices(List<InventoryItemResponseDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        var pricesByClassId = loadPricesForDistinctClassIds(items);
        applyPricesToItems(items, pricesByClassId);
    }

    private Map<String, BigDecimal> loadPricesForDistinctClassIds(List<InventoryItemResponseDto> items) {
        var result = new HashMap<String, BigDecimal>();
        var requested = new HashSet<String>();
        for (var item : items) {
            var cid = normalizedClassId(item);
            if (cid == null) {
                continue;
            }
            if (!requested.add(cid)) {
                continue;
            }
            var price = itemsCatalogClient.fetchMinPriceUsdBlocking(cid);
            if (price != null) {
                result.put(cid, price);
            }
        }
        return result;
    }

    private static void applyPricesToItems(List<InventoryItemResponseDto> items, Map<String, BigDecimal> pricesByClassId) {
        for (var item : items) {
            var cid = normalizedClassId(item);
            if (cid == null) {
                continue;
            }
            var p = pricesByClassId.get(cid);
            if (p != null) {
                item.setCatalogMinPriceUsd(p);
            }
        }
    }

    private static String normalizedClassId(InventoryItemResponseDto item) {
        if (item.getClassId() == null || item.getClassId().isBlank()) {
            return null;
        }
        return item.getClassId().trim();
    }
}
