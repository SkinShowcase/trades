package com.skinsshowcase.trades.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Выбранный предмет из инвентаря (asset + class для идентификации)")
public class SelectedItemDto {

    @NotBlank(message = "assetId обязателен")
    @Schema(description = "Steam asset id предмета в инвентаре", example = "12345678901", requiredMode = Schema.RequiredMode.REQUIRED)
    private String assetId;

    @NotBlank(message = "classId обязателен")
    @Schema(description = "Steam class id предмета (item_id)", example = "310776785", requiredMode = Schema.RequiredMode.REQUIRED)
    private String classId;
}
