package com.skinsshowcase.trades.repository;

import com.skinsshowcase.trades.entity.TradeSelection;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TradeSelectionRepository extends JpaRepository<TradeSelection, Long> {

    @Query("SELECT ts FROM TradeSelection ts LEFT JOIN FETCH ts.items WHERE ts.steamId = :steamId")
    Optional<TradeSelection> findBySteamIdWithItems(String steamId);

    @Query(
            value = "SELECT ts FROM TradeSelection ts WHERE size(ts.items) > 0 ORDER BY ts.updatedAt DESC",
            countQuery = "SELECT COUNT(ts) FROM TradeSelection ts WHERE size(ts.items) > 0"
    )
    Page<TradeSelection> findTradeSelectionsWithItemsOrderByUpdatedAtDesc(Pageable pageable);

    @Query(
            value = "SELECT ts FROM TradeSelection ts WHERE size(ts.items) > 0 AND ts.steamId <> :excludeSteamId ORDER BY ts.updatedAt DESC",
            countQuery = "SELECT COUNT(ts) FROM TradeSelection ts WHERE size(ts.items) > 0 AND ts.steamId <> :excludeSteamId"
    )
    Page<TradeSelection> findTradeSelectionsWithItemsOrderByUpdatedAtDescExcludingSteamId(String excludeSteamId, Pageable pageable);

    @Query("SELECT DISTINCT ts FROM TradeSelection ts LEFT JOIN FETCH ts.items WHERE size(ts.items) > 0 ORDER BY ts.updatedAt DESC")
    List<TradeSelection> findAllNonEmptySelectionsWithItemsOrderByUpdatedAtDesc();

    @Query("SELECT DISTINCT ts FROM TradeSelection ts LEFT JOIN FETCH ts.items WHERE size(ts.items) > 0 AND ts.steamId <> :excludeSteamId ORDER BY ts.updatedAt DESC")
    List<TradeSelection> findAllNonEmptySelectionsWithItemsOrderByUpdatedAtDescExcludingSteamId(String excludeSteamId);

    boolean existsBySteamId(String steamId);

    void deleteBySteamId(String steamId);
}
