package com.skinsshowcase.trades.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на создание/обновление набора предметов для обмена")
public class TradeSelectionRequestDto {

    @NotNull(message = "Список предметов обязателен")
    @Valid
    @Size(max = 5, message = "В наборе для обмена не более 5 предметов")
    @Schema(description = "Список выбранных предметов (порядок сохраняется), не более 5", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SelectedItemDto> items;
}
