package com.skinsshowcase.trades.dto.auth;

import java.util.Map;

public record AuthProfileLabelsResponseDto(Map<String, String> labelBySteamId) {
}
