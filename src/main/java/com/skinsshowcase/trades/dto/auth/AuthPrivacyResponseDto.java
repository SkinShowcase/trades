package com.skinsshowcase.trades.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthPrivacyResponseDto(@JsonProperty("private") boolean privateProfile) {
}
