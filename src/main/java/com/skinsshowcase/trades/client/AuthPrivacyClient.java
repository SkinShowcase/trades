package com.skinsshowcase.trades.client;

import com.skinsshowcase.trades.config.AuthServiceProperties;
import com.skinsshowcase.trades.dto.auth.AuthPrivacyFlagsRequestDto;
import com.skinsshowcase.trades.dto.auth.AuthPrivacyFlagsResponseDto;
import com.skinsshowcase.trades.dto.auth.AuthPrivacyResponseDto;
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

/**
 * Внутренний API auth: флаг приватности профиля (витрина / лента trades).
 */
@Component
public class AuthPrivacyClient {

    private static final Logger log = LoggerFactory.getLogger(AuthPrivacyClient.class);
    private static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";
    private static final String PRIVACY_PATH = "/auth/internal/users/{steamId}/privacy";
    private static final String PRIVACY_FLAGS_PATH = "/auth/internal/users/privacy-flags";
    private static final int BATCH_MAX = 200;

    private final WebClient webClient;
    private final AuthServiceProperties properties;

    public AuthPrivacyClient(@Qualifier("authServiceWebClient") WebClient webClient, AuthServiceProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    /**
     * {@code true}, если в auth у пользователя включена приватность; при ошибке или отсутствии пользователя — {@code false}.
     */
    public boolean isProfilePrivate(String steamId) {
        if (steamId == null || steamId.isBlank()) {
            return false;
        }
        var trimmed = steamId.trim();
        try {
            var spec = webClient.get().uri(PRIVACY_PATH, trimmed);
            addInternalKeyIfPresent(spec);
            var dto = spec.retrieve()
                    .bodyToMono(AuthPrivacyResponseDto.class)
                    .block();
            return dto != null && dto.privateProfile();
        } catch (WebClientResponseException.NotFound e) {
            return false;
        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("Auth internal privacy: 401 — проверьте AUTH_INTERNAL_SERVICE_KEY на auth и trades");
            return false;
        } catch (Exception e) {
            log.warn("Auth internal privacy failed for steamId={}: {}", trimmed, e.getMessage());
            return false;
        }
    }

    /**
     * Батч: значение {@code true} — профиль приватный. Отсутствующий ключ трактуется как не приватный.
     */
    public Map<String, Boolean> fetchPrivateFlags(List<String> steamIds) {
        var out = new HashMap<String, Boolean>();
        if (steamIds == null || steamIds.isEmpty()) {
            return out;
        }
        var unique = dedupeSteamIds(steamIds);
        var list = new ArrayList<String>(unique);
        for (var start = 0; start < list.size(); start += BATCH_MAX) {
            var end = Math.min(start + BATCH_MAX, list.size());
            var chunk = list.subList(start, end);
            mergePrivacyChunk(out, chunk);
        }
        return out;
    }

    private void mergePrivacyChunk(Map<String, Boolean> out, List<String> chunk) {
        if (chunk.isEmpty()) {
            return;
        }
        try {
            var spec = webClient.post()
                    .uri(PRIVACY_FLAGS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new AuthPrivacyFlagsRequestDto(new ArrayList<>(chunk)));
            addInternalKeyIfPresent(spec);
            var resp = spec.retrieve()
                    .bodyToMono(AuthPrivacyFlagsResponseDto.class)
                    .block();
            if (resp != null && resp.privateBySteamId() != null) {
                out.putAll(resp.privateBySteamId());
            }
        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("Auth internal privacy-flags: 401 — проверьте AUTH_INTERNAL_SERVICE_KEY");
        } catch (Exception e) {
            log.warn("Auth internal privacy-flags failed: {}", e.getMessage());
        }
    }

    private void addInternalKeyIfPresent(WebClient.RequestHeadersSpec<?> spec) {
        var key = properties.getInternalServiceKey();
        if (key == null || key.isBlank()) {
            return;
        }
        spec.header(INTERNAL_KEY_HEADER, key);
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
