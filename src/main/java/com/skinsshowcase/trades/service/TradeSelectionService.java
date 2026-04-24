package com.skinsshowcase.trades.service;

import com.skinsshowcase.trades.dto.InventoryItemResponseDto;
import com.skinsshowcase.trades.dto.InventoryResponseDto;
import com.skinsshowcase.trades.dto.SelectedItemDto;
import com.skinsshowcase.trades.dto.TradeFeedSetResponseDto;
import com.skinsshowcase.trades.dto.TradeSelectionItemFilter;
import com.skinsshowcase.trades.dto.TradeSelectionRequestDto;
import com.skinsshowcase.trades.dto.TradeSelectionResponseDto;
import com.skinsshowcase.trades.client.AuthPrivacyClient;
import com.skinsshowcase.trades.client.AuthProfileLabelsClient;
import com.skinsshowcase.trades.entity.TradeSelection;
import com.skinsshowcase.trades.entity.TradeSelectionItem;
import com.skinsshowcase.trades.exception.InvalidTradeSelectionException;
import com.skinsshowcase.trades.exception.TradeSelectionNotFoundException;
import com.skinsshowcase.trades.metrics.TradesMetrics;
import com.skinsshowcase.trades.repository.TradeSelectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Сервис набора предметов для обмена: создание/обновление с проверкой по инвентарю (steam-gateway), чтение, удаление.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeSelectionService {

    private final TradeSelectionRepository tradeSelectionRepository;
    private final SteamGatewayClientAdapter steamGatewayClientAdapter;
    private final CatalogPriceEnricher catalogPriceEnricher;
    private final AuthPrivacyClient authPrivacyClient;
    private final AuthProfileLabelsClient authProfileLabelsClient;
    private final TradesMetrics tradesMetrics;
    private final SteamIdProtectionService steamIdProtectionService;

    /**
     * Создаёт или обновляет набор предметов для обмена. Проверяет, что все указанные предметы есть в инвентаре пользователя (steam-gateway).
     */
    @Transactional
    public TradeSelectionResponseDto createOrUpdate(String steamId, TradeSelectionRequestDto request) {
        validateItemsBelongToUser(steamId, request.getItems());
        var steamIdHash = hashSteamId(steamId);
        var entity = tradeSelectionRepository.findBySteamIdWithItems(steamIdHash)
                .orElseGet(() -> new TradeSelection(steamIdHash, encryptSteamId(steamId)));
        entity.setSteamIdEnc(encryptSteamId(steamId));
        entity.setUpdatedAt(Instant.now());
        replaceItems(entity, request.getItems());
        var saved = tradeSelectionRepository.save(entity);
        tradesMetrics.recordSelectionOperation("create_or_update");
        return TradeSelectionResponseDto.from(saved, resolvePersonaLabelForSteamId(steamId), steamId);
    }

    /**
     * Набор по steam_id с учётом приватности: чужой приватный профиль — как отсутствие набора (404).
     *
     * @param viewerSteamIdOrNull subject JWT или null (аноним)
     */
    @Transactional(readOnly = true)
    public TradeSelectionResponseDto getBySteamIdForViewer(String steamId, String viewerSteamIdOrNull) {
        assertShowcaseVisibleForViewer(steamId, viewerSteamIdOrNull);
        var dto = loadSelectionOrThrow(steamId);
        tradesMetrics.recordSelectionOperation("get_viewer");
        return dto;
    }

    private void assertShowcaseVisibleForViewer(String ownerSteamId, String viewerSteamIdOrNull) {
        if (viewerSteamIdOrNull != null && viewerSteamIdOrNull.equals(ownerSteamId)) {
            return;
        }
        if (authPrivacyClient.isProfilePrivate(ownerSteamId)) {
            throw new TradeSelectionNotFoundException("Trade selection not found for steamId: " + ownerSteamId);
        }
    }

    private TradeSelectionResponseDto loadSelectionOrThrow(String steamId) {
        var entity = tradeSelectionRepository.findBySteamIdWithItems(hashSteamId(steamId))
                .orElseThrow(() -> new TradeSelectionNotFoundException("Trade selection not found for steamId: " + steamId));
        return TradeSelectionResponseDto.from(entity, resolvePersonaLabelForSteamId(steamId), steamId);
    }

    private String resolvePersonaLabelForSteamId(String steamId) {
        var labelMap = authProfileLabelsClient.fetchProfileLabels(List.of(steamId));
        return lookupProfileLabel(labelMap, steamId);
    }

    /**
     * Удаляет набор предметов для обмена по steam_id.
     */
    @Transactional
    public void deleteBySteamId(String steamId) {
        tradeSelectionRepository.deleteBySteamId(hashSteamId(steamId));
        tradesMetrics.recordSelectionOperation("delete");
    }

    /**
     * Возвращает ленту предложений для обмена (наборы других пользователей с хотя бы одним предметом).
     * В каждом элементе {@link TradeSelectionResponseDto#getSteamId()} — владелец набора.
     * Сортировка по дате обновления (новые выше). Опционально исключает набор текущего пользователя.
     */
    @Transactional(readOnly = true)
    public Page<TradeSelectionResponseDto> getFeed(Pageable pageable, String excludeSteamId) {
        var page = isExcludeSteamId(excludeSteamId)
                ? tradeSelectionRepository.findTradeSelectionsWithItemsOrderByUpdatedAtDescExcludingSteamId(hashSteamId(excludeSteamId), pageable)
                : tradeSelectionRepository.findTradeSelectionsWithItemsOrderByUpdatedAtDesc(pageable);
        var steamIdByHash = decryptSteamIds(page.getContent());
        var flags = authPrivacyClient.fetchPrivateFlags(extractSteamIdsFromTradeSelections(page.getContent(), steamIdByHash));
        var visible = filterTradeSelectionsByPublicProfile(page.getContent(), flags, steamIdByHash);
        var labelMap = authProfileLabelsClient.fetchProfileLabels(extractSteamIdsFromTradeSelections(visible, steamIdByHash));
        var dtos = mapTradeSelectionsToDtos(visible, labelMap, steamIdByHash);
        tradesMetrics.recordSelectionOperation("feed_page");
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    /**
     * Наборы из ленты (непустые), у которых хотя бы один предмет в инвентаре проходит фильтр.
     * В ответе каждый набор целиком: все предметы набора, найденные в текущем инвентаре Steam, с ценой каталога для фильтра по цене.
     */
    @Transactional(readOnly = true)
    public List<TradeFeedSetResponseDto> getFilteredFeedSets(TradeSelectionItemFilter filter, String excludeSteamId) {
        var selections = loadAllNonEmptySelectionsForFeed(excludeSteamId);
        if (selections.isEmpty()) {
            tradesMetrics.recordSelectionOperation("feed_filtered");
            return List.of();
        }
        var steamIdByHash = decryptSteamIds(selections);
        var flags = authPrivacyClient.fetchPrivateFlags(extractSteamIdsFromTradeSelections(selections, steamIdByHash));
        var visible = filterTradeSelectionsByPublicProfile(selections, flags, steamIdByHash);
        if (visible.isEmpty()) {
            tradesMetrics.recordSelectionOperation("feed_filtered");
            return List.of();
        }
        var labelMap = authProfileLabelsClient.fetchProfileLabels(extractSteamIdsFromTradeSelections(visible, steamIdByHash));
        var sets = collectMatchingFeedSets(visible, filter, labelMap, steamIdByHash);
        tradesMetrics.recordSelectionOperation("feed_filtered");
        return sets;
    }

    private static List<String> extractSteamIdsFromTradeSelections(List<TradeSelection> selections, Map<String, String> steamIdByHash) {
        var out = new ArrayList<String>();
        if (selections == null) {
            return out;
        }
        for (var s : selections) {
            var steamId = steamIdByHash.get(s.getSteamId());
            if (steamId != null && !steamId.isBlank()) {
                out.add(steamId);
            }
        }
        return out;
    }

    private static List<TradeSelection> filterTradeSelectionsByPublicProfile(List<TradeSelection> selections,
                                                                             Map<String, Boolean> privateFlags,
                                                                             Map<String, String> steamIdByHash) {
        var out = new ArrayList<TradeSelection>();
        for (var s : selections) {
            if (!isPrivateOwner(steamIdByHash.get(s.getSteamId()), privateFlags)) {
                out.add(s);
            }
        }
        return out;
    }

    private static boolean isPrivateOwner(String steamId, Map<String, Boolean> privateFlags) {
        if (steamId == null) {
            return true;
        }
        return Boolean.TRUE.equals(privateFlags.get(steamId.trim()));
    }

    private static List<TradeSelectionResponseDto> mapTradeSelectionsToDtos(List<TradeSelection> entities,
                                                                            Map<String, String> labelBySteamId,
                                                                            Map<String, String> steamIdByHash) {
        var out = new ArrayList<TradeSelectionResponseDto>();
        for (var e : entities) {
            var steamId = steamIdByHash.get(e.getSteamId());
            out.add(TradeSelectionResponseDto.from(e, lookupProfileLabel(labelBySteamId, steamId), steamId));
        }
        return out;
    }

    private static String lookupProfileLabel(Map<String, String> labelBySteamId, String steamId) {
        if (steamId == null || labelBySteamId == null) {
            return null;
        }
        var v = labelBySteamId.get(steamId.trim());
        if (v == null || v.isBlank()) {
            return null;
        }
        return v.trim();
    }

    private List<TradeFeedSetResponseDto> collectMatchingFeedSets(List<TradeSelection> selections,
                                                                   TradeSelectionItemFilter filter,
                                                                   Map<String, String> labelBySteamId,
                                                                   Map<String, String> steamIdByHash) {
        var result = new ArrayList<TradeFeedSetResponseDto>();
        for (var selection : selections) {
            var setDto = buildFilteredFeedSetOrNull(selection, filter, labelBySteamId, steamIdByHash);
            if (setDto != null) {
                result.add(setDto);
            }
        }
        return result;
    }

    private TradeFeedSetResponseDto buildFilteredFeedSetOrNull(TradeSelection selection, TradeSelectionItemFilter filter,
                                                               Map<String, String> labelBySteamId,
                                                               Map<String, String> steamIdByHash) {
        var steamId = steamIdByHash.get(selection.getSteamId());
        if (steamId == null || steamId.isBlank()) {
            return null;
        }
        var inventory = steamGatewayClientAdapter.getInventoryBlocking(steamId);
        var keys = buildSelectionKeySet(selection);
        var items = filterInventoryBySelection(inventory, keys);
        if (items.isEmpty()) {
            return null;
        }
        catalogPriceEnricher.enrichCatalogPrices(items);
        if (!hasAnyItemMatchingFilter(items, filter)) {
            return null;
        }
        var personaName = lookupProfileLabel(labelBySteamId, steamId);
        return TradeFeedSetResponseDto.fromSelection(selection, items, personaName, steamId);
    }

    private static boolean hasAnyItemMatchingFilter(List<InventoryItemResponseDto> items, TradeSelectionItemFilter filter) {
        for (var item : items) {
            if (TradeSelectionItemFilterHelper.matches(item, filter)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> buildSelectionKeySet(TradeSelection selection) {
        var keys = new HashSet<String>();
        mergeSelectionKeys(keys, selection);
        return keys;
    }

    private List<TradeSelection> loadAllNonEmptySelectionsForFeed(String excludeSteamId) {
        if (isExcludeSteamId(excludeSteamId)) {
            return tradeSelectionRepository.findAllNonEmptySelectionsWithItemsOrderByUpdatedAtDescExcludingSteamId(hashSteamId(excludeSteamId.trim()));
        }
        return tradeSelectionRepository.findAllNonEmptySelectionsWithItemsOrderByUpdatedAtDesc();
    }

    private static void mergeSelectionKeys(Set<String> keys, TradeSelection selection) {
        if (selection.getItems() == null) {
            return;
        }
        for (var item : selection.getItems()) {
            keys.add(assetClassKey(item.getAssetId(), item.getClassId()));
        }
    }

    private static List<InventoryItemResponseDto> filterInventoryBySelection(InventoryResponseDto inventory, Set<String> selectionKeys) {
        if (inventory == null || inventory.getItems() == null) {
            return List.of();
        }
        var result = new ArrayList<InventoryItemResponseDto>();
        for (var item : inventory.getItems()) {
            if (selectionKeys.contains(assetClassKey(item.getAssetId(), item.getClassId()))) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Удаляет указанные предметы из набора пользователя. Если набора нет — 404.
     */
    @Transactional
    public TradeSelectionResponseDto removeItems(String steamId, List<SelectedItemDto> itemsToRemove) {
        var entity = tradeSelectionRepository.findBySteamIdWithItems(hashSteamId(steamId))
                .orElseThrow(() -> new TradeSelectionNotFoundException("Trade selection not found for steamId: " + steamId));
        var keysToRemove = buildAssetClassSetFromSelected(itemsToRemove);
        removeMatchingItems(entity, keysToRemove);
        entity.setUpdatedAt(Instant.now());
        var saved = tradeSelectionRepository.save(entity);
        tradesMetrics.recordSelectionOperation("remove_items");
        return TradeSelectionResponseDto.from(saved, resolvePersonaLabelForSteamId(steamId), steamId);
    }

    private static boolean isExcludeSteamId(String excludeSteamId) {
        return excludeSteamId != null && !excludeSteamId.isBlank();
    }

    private static Set<String> buildAssetClassSetFromSelected(List<SelectedItemDto> items) {
        var set = new HashSet<String>();
        if (items == null || items.isEmpty()) {
            return set;
        }
        for (var dto : items) {
            set.add(assetClassKey(dto.getAssetId(), dto.getClassId()));
        }
        return set;
    }

    private void removeMatchingItems(TradeSelection entity, Set<String> keysToRemove) {
        if (keysToRemove.isEmpty()) {
            return;
        }
        entity.getItems().removeIf(item -> keysToRemove.contains(assetClassKey(item.getAssetId(), item.getClassId())));
    }

    private void validateItemsBelongToUser(String steamId, List<SelectedItemDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        var inventory = steamGatewayClientAdapter.getInventoryBlocking(steamId);
        var allowedSet = buildAssetClassSet(inventory);
        for (var item : items) {
            if (!allowedSet.contains(assetClassKey(item.getAssetId(), item.getClassId()))) {
                throw new InvalidTradeSelectionException(
                        "Item not in user inventory or inventory unavailable: assetId=" + item.getAssetId() + ", classId=" + item.getClassId());
            }
        }
    }

    private Set<String> buildAssetClassSet(InventoryResponseDto inventory) {
        var set = new HashSet<String>();
        if (inventory == null || inventory.getItems() == null) {
            return set;
        }
        for (var i : inventory.getItems()) {
            set.add(assetClassKey(i.getAssetId(), i.getClassId()));
        }
        return set;
    }

    private static String assetClassKey(String assetId, String classId) {
        return (assetId != null ? assetId : "") + "|" + (classId != null ? classId : "");
    }

    private Map<String, String> decryptSteamIds(List<TradeSelection> selections) {
        var out = new java.util.HashMap<String, String>();
        for (var selection : selections) {
            out.put(selection.getSteamId(), decryptSteamId(selection));
        }
        return out;
    }

    private String decryptSteamId(TradeSelection selection) {
        return steamIdProtectionService.decrypt(selection.getSteamIdEnc());
    }

    private String hashSteamId(String steamId) {
        return steamIdProtectionService.hash(steamId);
    }

    private String encryptSteamId(String steamId) {
        return steamIdProtectionService.encrypt(steamId);
    }

    private void replaceItems(TradeSelection entity, List<SelectedItemDto> items) {
        entity.getItems().clear();
        if (items == null || items.isEmpty()) {
            return;
        }
        var order = 0;
        for (var dto : items) {
            entity.getItems().add(new TradeSelectionItem(entity, dto.getAssetId(), dto.getClassId(), order++));
        }
    }
}
