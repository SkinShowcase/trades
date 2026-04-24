package com.skinsshowcase.trades.dto;

import com.skinsshowcase.trades.entity.TradeSelection;
import com.skinsshowcase.trades.entity.TradeSelectionItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Набор предметов пользователя для обмена")
public class TradeSelectionResponseDto {

    @Schema(description = "Идентификатор набора")
    private Long id;

    @Schema(description = "Steam ID владельца")
    private String steamId;

    @Schema(description = "Отображаемое имя владельца из auth (display_name, иначе persona_name в БД); null, если пользователь не найден в auth или лейбл пуст")
    private String personaName;

    @Schema(description = "Выбранные предметы")
    private List<SelectedItemDto> items;

    @Schema(description = "Дата создания")
    private Instant createdAt;

    @Schema(description = "Дата последнего обновления")
    private Instant updatedAt;

    public static TradeSelectionResponseDto from(TradeSelection entity) {
        return from(entity, null);
    }

    public static TradeSelectionResponseDto from(TradeSelection entity, String personaName) {
        return from(entity, personaName, entity.getSteamId());
    }

    public static TradeSelectionResponseDto from(TradeSelection entity, String personaName, String steamId) {
        var itemDtos = entity.getItems().stream()
                .map(TradeSelectionResponseDto::toSelectedItemDto)
                .collect(Collectors.toList());
        return TradeSelectionResponseDto.builder()
                .id(entity.getId())
                .steamId(steamId)
                .personaName(personaName)
                .items(itemDtos)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static SelectedItemDto toSelectedItemDto(TradeSelectionItem item) {
        return SelectedItemDto.builder()
                .assetId(item.getAssetId())
                .classId(item.getClassId())
                .build();
    }
}
