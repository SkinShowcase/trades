package com.skinsshowcase.trades.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на удаление предметов из набора для обмена")
public class RemoveItemsRequestDto {

    @NotNull(message = "Список предметов для удаления обязателен")
    @Valid
    @Schema(description = "Список предметов (assetId + classId) для удаления из набора", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SelectedItemDto> items;
}
