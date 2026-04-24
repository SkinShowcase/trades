package com.skinsshowcase.trades.client;

import com.skinsshowcase.trades.config.AuthServiceProperties;
import com.skinsshowcase.trades.dto.auth.AuthProfileLabelsRequestDto;
import com.skinsshowcase.trades.dto.auth.AuthProfileLabelsResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
public class AuthProfileLabelsClient {

    private static final Logger log = LoggerFactory.getLogger(AuthProfileLabelsClient.class);
    private static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";
    private static final String PROFILE_LABELS_PATH = "/auth/internal/users/profile-labels";
    private static final int BATCH_MAX = 200;

    private final WebClient webClient;
    private final AuthServiceProperties properties;

    public AuthProfileLabelsClient(@Qualifier("authServiceWebClient") WebClient webClient, AuthServiceProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public Map<String, String> fetchProfileLabels(List<String> steamIds) {
        var out = new HashMap<String, String>();
        if (steamIds == null || steamIds.isEmpty()) {
            return out;
        }
        var unique = dedupeSteamIds(steamIds);
        var list = new ArrayList<String>(unique);
        for (var start = 0; start < list.size(); start += BATCH_MAX) {
            var end = Math.min(start + BATCH_MAX, list.size());
            var chunk = list.subList(start, end);
            mergeLabelsChunk(out, chunk);
        }
        return out;
    }

    private void mergeLabelsChunk(Map<String, String> out, List<String> chunk) {
        if (chunk.isEmpty()) {
            return;
        }
        try {
            var body = new AuthProfileLabelsRequestDto(new ArrayList<>(chunk));
            var spec = webClient.post()
                    .uri(PROFILE_LABELS_PATH)
                    .contentType(MediaType.APPLICATION_JSON);
            spec = withInternalKeyHeader(spec);
            var resp = spec.bodyValue(body)
                    .retrieve()
                    .bodyToMono(AuthProfileLabelsResponseDto.class)
                    .block();
            if (resp != null && resp.labelBySteamId() != null) {
                out.putAll(resp.labelBySteamId());
            }
        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("Auth internal profile-labels: 401 — проверьте AUTH_INTERNAL_SERVICE_KEY");
        } catch (Exception e) {
            log.warn("Auth profile-labels failed: {}", e.getMessage());
        }
    }

    private WebClient.RequestBodySpec withInternalKeyHeader(WebClient.RequestBodySpec spec) {
        var key = properties.getInternalServiceKey();
        if (key == null || key.isBlank()) {
            return spec;
        }
        return spec.header(INTERNAL_KEY_HEADER, key);
    }

    private static LinkedHashSet<String> dedupeSteamIds(List<String> steamIds) {
        var out = new LinkedHashSet<String>();
        for (var sid : steamIds) {
            if (sid != null && !sid.isBlank()) {
                out.add(sid.trim());
            }
        }
        return out;
    }
}
