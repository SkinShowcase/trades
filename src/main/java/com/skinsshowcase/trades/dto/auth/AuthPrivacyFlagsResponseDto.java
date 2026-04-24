package com.skinsshowcase.trades.dto.auth;

import java.util.Map;

public record AuthPrivacyFlagsResponseDto(Map<String, Boolean> privateBySteamId) {
}
