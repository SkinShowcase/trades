package com.skinsshowcase.trades.exception;

/**
 * Выбранные предметы не принадлежат инвентарю пользователя или инвентарь недоступен.
 */
public class InvalidTradeSelectionException extends RuntimeException {

    public InvalidTradeSelectionException(String message) {
        super(message);
    }

    public InvalidTradeSelectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
