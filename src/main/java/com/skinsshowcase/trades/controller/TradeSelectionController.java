package com.skinsshowcase.trades.controller;

import com.skinsshowcase.trades.dto.RemoveItemsRequestDto;
import com.skinsshowcase.trades.dto.SpecialFilter;
import com.skinsshowcase.trades.dto.TradeFeedSetResponseDto;
import com.skinsshowcase.trades.dto.TradeSelectionItemFilter;
import com.skinsshowcase.trades.dto.TradeSelectionRequestDto;
import com.skinsshowcase.trades.dto.TradeSelectionResponseDto;
import com.skinsshowcase.trades.service.JwtSupportService;
import com.skinsshowcase.trades.service.TradeSelectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trades")
@Validated
@Tag(name = "Trade selection", description = "Набор предметов из инвентаря пользователя для обмена")
@RequiredArgsConstructor
public class TradeSelectionController {

    private static final String STEAM_ID64_PATTERN = "^765[0-9]{14}$";

    private final TradeSelectionService tradeSelectionService;
    private final JwtSupportService jwtSupportService;

    @PutMapping(value = "/selection/{steamId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Создать или обновить набор предметов для обмена",
            description = "Один набор на пользователя, не более 5 предметов. Все предметы должны быть в инвентаре (проверка через steam-gateway). В ответе steamId — владелец."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Набор сохранён",
                    content = @Content(schema = @Schema(implementation = TradeSelectionResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Невалидный запрос или предметы не из инвентаря"),
            @ApiResponse(responseCode = "502", description = "Ошибка steam-gateway (инвентарь недоступен)")
    })
    public TradeSelectionResponseDto upsertSelection(
            @Parameter(description = "Steam ID пользователя (SteamID64)", required = true, example = "76561198000000000")
            @PathVariable @Pattern(regexp = STEAM_ID64_PATTERN, message = "Steam ID должен быть в формате SteamID64 (17 цифр, 765...)") String steamId,
            @Valid @RequestBody TradeSelectionRequestDto request
    ) {
        return tradeSelectionService.createOrUpdate(steamId, request);
    }

    @GetMapping(value = "/showcase/{steamId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Набор пользователя (витрина) по Steam ID",
            description = "То же, что GET /selection/{steamId}: один набор до 5 предметов с привязкой к steamId владельца. " +
                    "Если профиль владельца приватный в auth, чужие пользователи получают 404; владелец может передать Authorization: Bearer (тот же steamId в JWT)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Набор найден",
                    content = @Content(schema = @Schema(implementation = TradeSelectionResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Набор не найден или витрина скрыта приватным профилем")
    })
    public TradeSelectionResponseDto getShowcaseBySteamId(
            @Parameter(description = "Steam ID пользователя", required = true, example = "76561198000000000")
            @PathVariable @Pattern(regexp = STEAM_ID64_PATTERN, message = "Steam ID должен быть в формате SteamID64") String steamId,
            @Parameter(description = "Опционально: JWT владельца, чтобы видеть свою витрину при приватном профиле")
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        var viewer = jwtSupportService.tryParseSteamIdFromAuthorization(authorization);
        return tradeSelectionService.getBySteamIdForViewer(steamId, viewer);
    }

    @GetMapping(value = "/selection/{steamId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Получить набор предметов для обмена",
            description = "Возвращает сохранённый набор по steam_id (до 5 предметов). Поле steamId — владелец набора. " +
                    "При приватном профиле владельца для чужих — 404; владелец может передать Bearer JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Набор найден",
                    content = @Content(schema = @Schema(implementation = TradeSelectionResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Набор не найден или скрыт приватным профилем")
    })
    public TradeSelectionResponseDto getSelection(
            @Parameter(description = "Steam ID пользователя", required = true, example = "76561198000000000")
            @PathVariable @Pattern(regexp = STEAM_ID64_PATTERN, message = "Steam ID должен быть в формате SteamID64") String steamId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        var viewer = jwtSupportService.tryParseSteamIdFromAuthorization(authorization);
        return tradeSelectionService.getBySteamIdForViewer(steamId, viewer);
    }

    @GetMapping(value = "/feed/sets/filtered", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Наборы из ленты с фильтрами",
            description = "Берёт непустые наборы для обмена (как GET /feed); владельцы с приватным профилем исключаются. Возвращает только те наборы, где хотя бы один предмет из текущего инвентаря владельца удовлетворяет фильтрам. " +
                    "В каждом элементе ответа поле steamId — владелец; items — все предметы набора из инвентаря (до 5), с полем catalogMinPriceUsd из каталога items для фильтрации по цене. " +
                    "minFloat/maxFloat по умолчанию 0 и 1. minPriceUsd/maxPriceUsd — USD из каталога; при фильтре по цене предметы без цены в каталоге не считаются совпадением. " +
                    "excludeSteamId — не включать набор этого пользователя (как у GET /feed)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список наборов; пустой, если никто не подошёл",
                    content = @Content(schema = @Schema(implementation = TradeFeedSetResponseDto.class))),
            @ApiResponse(responseCode = "502", description = "Ошибка steam-gateway или items (частично: цены могут отсутствовать)")
    })
    public List<TradeFeedSetResponseDto> getFilteredFeedSets(
            @Parameter(description = "Минимальный float (0–1). По умолчанию 0")
            @RequestParam(required = false) Double minFloat,
            @Parameter(description = "Максимальный float (0–1). По умолчанию 1")
            @RequestParam(required = false) Double maxFloat,
            @Parameter(description = "Подстрока в названии скина")
            @RequestParam(required = false) String name,
            @Parameter(description = "Подстрока в названии коллекции")
            @RequestParam(required = false) String collectionName,
            @Parameter(description = "Подстрока в названии износа (Factory New, Field-Tested и т.д.)")
            @RequestParam(required = false) String wearName,
            @Parameter(description = "Номер паттерна")
            @RequestParam(required = false) Integer pattern,
            @Parameter(description = "Редкость: подстрока в type (consumer, industrial, mil-spec, restricted, classified, covert, contraband, extraordinary)")
            @RequestParam(required = false) String type,
            @Parameter(description = "Special: STATTRACK, SOUVENIR, NORMAL")
            @RequestParam(required = false) SpecialFilter special,
            @Parameter(description = "Минимальная цена USD (каталог items)")
            @RequestParam(required = false) BigDecimal minPriceUsd,
            @Parameter(description = "Максимальная цена USD (каталог items)")
            @RequestParam(required = false) BigDecimal maxPriceUsd,
            @Parameter(description = "Steam ID — набор этого пользователя не попадёт в выборку")
            @RequestParam(required = false) String excludeSteamId
    ) {
        var filter = buildItemFilter(minFloat, maxFloat, name, collectionName, wearName, pattern, type, special, minPriceUsd, maxPriceUsd);
        return tradeSelectionService.getFilteredFeedSets(filter, excludeSteamId);
    }

    private static TradeSelectionItemFilter buildItemFilter(Double minFloat, Double maxFloat,
                                                           String name, String collectionName, String wearName,
                                                           Integer pattern, String type, SpecialFilter special,
                                                           BigDecimal minPriceUsd, BigDecimal maxPriceUsd) {
        return TradeSelectionItemFilter.builder()
                .minFloat(minFloat != null ? minFloat : 0.0)
                .maxFloat(maxFloat != null ? maxFloat : 1.0)
                .name(trimToNull(name))
                .collectionName(trimToNull(collectionName))
                .wearName(trimToNull(wearName))
                .pattern(pattern)
                .type(trimToNull(type))
                .special(special)
                .minPriceUsd(minPriceUsd)
                .maxPriceUsd(maxPriceUsd)
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @DeleteMapping(value = "/selection/{steamId}")
    @Operation(summary = "Удалить набор предметов для обмена")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Набор удалён")
    })
    public void deleteSelection(
            @Parameter(description = "Steam ID пользователя", required = true, example = "76561198000000000")
            @PathVariable @Pattern(regexp = STEAM_ID64_PATTERN, message = "Steam ID должен быть в формате SteamID64") String steamId
    ) {
        tradeSelectionService.deleteBySteamId(steamId);
    }

    @GetMapping(value = "/feed", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Лента предложений для обмена",
            description = "Страница наборов (по одному набору на пользователя, до 5 предметов в наборе). В каждом элементе поле steamId — владелец набора. Сортировка по дате обновления. Опционально исключает набор текущего пользователя. " +
                    "Наборы пользователей с приватным профилем в auth не попадают в ленту."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница предложений",
                    content = @Content(schema = @Schema(implementation = org.springframework.data.domain.Page.class)))
    })
    public Page<TradeSelectionResponseDto> getFeed(
            @Parameter(description = "Страница (0-based)")
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Steam ID текущего пользователя — его набор не попадёт в ленту")
            @RequestParam(required = false) String excludeSteamId
    ) {
        return tradeSelectionService.getFeed(pageable, excludeSteamId);
    }

    @DeleteMapping(value = "/selection/{steamId}/items", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Удалить предметы из набора для обмена",
            description = "Убирает указанные скины из списка «участвующих в обмене» — они перестают отображаться в ленте для других пользователей. " +
                    "Требуется Authorization: Bearer, subject токена должен совпадать с steamId в пути (см. TradesOwnerJwtFilter)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Набор обновлён",
                    content = @Content(schema = @Schema(implementation = TradeSelectionResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Невалидное тело запроса"),
            @ApiResponse(responseCode = "401", description = "Нет или невалидный JWT"),
            @ApiResponse(responseCode = "403", description = "JWT не относится к владельцу набора"),
            @ApiResponse(responseCode = "404", description = "Набор не найден")
    })
    public TradeSelectionResponseDto removeItemsFromSelection(
            @Parameter(description = "Steam ID пользователя", required = true, example = "76561198000000000")
            @PathVariable @Pattern(regexp = STEAM_ID64_PATTERN, message = "Steam ID должен быть в формате SteamID64") String steamId,
            @Valid @RequestBody RemoveItemsRequestDto request
    ) {
        return tradeSelectionService.removeItems(steamId, request.getItems());
    }
}
