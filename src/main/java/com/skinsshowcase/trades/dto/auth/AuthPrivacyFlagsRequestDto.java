package com.skinsshowcase.trades.dto.auth;

import java.util.List;

public record AuthPrivacyFlagsRequestDto(List<String> steamIds) {
}
