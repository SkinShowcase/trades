package com.skinsshowcase.trades.exception;

/**
 * Ошибка при обращении к steam-gateway (таймаут, 5xx, недоступность).
 */
public class SteamGatewayException extends RuntimeException {

    public SteamGatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    public SteamGatewayException(String message) {
        super(message);
    }
}
