package com.skinsshowcase.trades.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Выбор предметов пользователя для обмена: один «набор» на steam_id.
 */
@Entity
@Table(name = "trade_selection")
public class TradeSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "steam_id", nullable = false, unique = true, length = 64)
    private String steamId;

    @Column(name = "steam_id_enc", columnDefinition = "TEXT")
    private String steamIdEnc;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "tradeSelection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<TradeSelectionItem> items = new ArrayList<>();

    protected TradeSelection() {
    }

    public TradeSelection(String steamIdHash, String steamIdEnc) {
        this.steamId = Objects.requireNonNull(steamIdHash, "steamIdHash");
        this.steamIdEnc = Objects.requireNonNull(steamIdEnc, "steamIdEnc");
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getSteamId() {
        return steamId;
    }

    public String getSteamIdEnc() {
        return steamIdEnc;
    }

    public void setSteamIdEnc(String steamIdEnc) {
        this.steamIdEnc = steamIdEnc;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TradeSelectionItem> getItems() {
        return items;
    }

    public void setItems(List<TradeSelectionItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
