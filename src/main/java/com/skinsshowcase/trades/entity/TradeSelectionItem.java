package com.skinsshowcase.trades.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Один предмет в наборе для обмена (ссылка на asset в инвентаре Steam).
 */
@Entity
@Table(name = "trade_selection_item")
public class TradeSelectionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_selection_id", nullable = false)
    private TradeSelection tradeSelection;

    @Column(name = "asset_id", nullable = false, length = 64)
    private String assetId;

    @Column(name = "class_id", nullable = false, length = 64)
    private String classId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected TradeSelectionItem() {
    }

    public TradeSelectionItem(TradeSelection tradeSelection, String assetId, String classId, int sortOrder) {
        this.tradeSelection = Objects.requireNonNull(tradeSelection, "tradeSelection");
        this.assetId = Objects.requireNonNull(assetId, "assetId");
        this.classId = Objects.requireNonNull(classId, "classId");
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public TradeSelection getTradeSelection() {
        return tradeSelection;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getClassId() {
        return classId;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
