package com.skinsshowcase.trades.dto;

import com.skinsshowcase.trades.entity.TradeSelection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Набор для обмена в ленте с полными данными предметов (инвентарь + цена каталога).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Набор скинов пользователя в ленте: steamId владельца и предметы с атрибутами из инвентаря")
public class TradeFeedSetResponseDto {

    @Schema(description = "Идентификатор набора в БД")
    private Long id;

    @Schema(description = "Steam ID64 владельца набора", example = "76561198000000000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String steamId;

    @Schema(description = "Отображаемое имя владельца из auth (display_name / persona_name в БД)")
    private String personaName;

    @Schema(description = "Дата создания набора")
    private Instant createdAt;

    @Schema(description = "Дата последнего обновления")
    private Instant updatedAt;

    @Schema(description = "Предметы набора, найденные в текущем инвентаре Steam (до 5)")
    private List<InventoryItemResponseDto> items;

    public static TradeFeedSetResponseDto fromSelection(TradeSelection selection, List<InventoryItemResponseDto> inventoryItems,
                                                        String personaName) {
        return fromSelection(selection, inventoryItems, personaName, selection.getSteamId());
    }

    public static TradeFeedSetResponseDto fromSelection(TradeSelection selection, List<InventoryItemResponseDto> inventoryItems,
                                                        String personaName, String steamId) {
        var copy = new ArrayList<InventoryItemResponseDto>();
        if (inventoryItems != null) {
            copy.addAll(inventoryItems);
        }
        return TradeFeedSetResponseDto.builder()
                .id(selection.getId())
                .steamId(steamId)
                .personaName(personaName)
                .createdAt(selection.getCreatedAt())
                .updatedAt(selection.getUpdatedAt())
                .items(copy)
                .build();
    }
}
