package com.skinsshowcase.trades.exception;

/**
 * Ошибка при обращении к сервису items (404, 5xx, таймаут).
 */
public class ItemsClientException extends RuntimeException {

    public ItemsClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ItemsClientException(String message) {
        super(message);
    }
}
