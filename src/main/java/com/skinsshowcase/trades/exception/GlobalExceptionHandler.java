package com.skinsshowcase.trades.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TradeSelectionNotFoundException.class)
    public ProblemDetail handleNotFound(TradeSelectionNotFoundException e) {
        log.debug("Trade selection not found: {}", e.getMessage());
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        detail.setTitle("Trade Selection Not Found");
        return detail;
    }

    @ExceptionHandler(InvalidTradeSelectionException.class)
    public ProblemDetail handleInvalidSelection(InvalidTradeSelectionException e) {
        log.debug("Invalid trade selection: {}", e.getMessage());
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        detail.setTitle("Invalid Trade Selection");
        return detail;
    }

    @ExceptionHandler(SteamGatewayException.class)
    public ProblemDetail handleSteamGateway(SteamGatewayException e) {
        log.warn("Steam gateway error: {}", e.getMessage());
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
        detail.setTitle("Steam Gateway Error");
        return detail;
    }

    @ExceptionHandler(ItemsClientException.class)
    public ProblemDetail handleItemsClient(ItemsClientException e) {
        log.warn("Items client error: {}", e.getMessage());
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
        detail.setTitle("Items Service Error");
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException e) {
        log.debug("Bad request: {}", e.getMessage());
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        detail.setTitle("Bad Request");
        return detail;
    }
}
