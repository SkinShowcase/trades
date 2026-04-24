package com.skinsshowcase.trades.exception;

/**
 * Набор предметов для обмена по указанному steam_id не найден.
 */
public class TradeSelectionNotFoundException extends RuntimeException {

    public TradeSelectionNotFoundException(String message) {
        super(message);
    }
}
