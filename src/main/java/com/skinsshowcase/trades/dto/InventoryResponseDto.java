package com.skinsshowcase.trades.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ответ steam-gateway GET /api/v1/inventory/{steamId}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponseDto {

    private String steamId;
    /** См. steam-gateway GET /api/v1/inventory/{steamId} — опционально. */
    private String personaName;
    private Integer appId;
    private Integer contextId;
    private List<InventoryItemResponseDto> items;
}
