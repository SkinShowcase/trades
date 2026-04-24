package com.skinsshowcase.trades.filter;

import com.skinsshowcase.trades.service.JwtSupportService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Мутации набора для обмена только с JWT владельца (subject = steamId в пути):
 * {@code PUT /api/v1/trades/selection/{steamId}} и любой {@code DELETE} на путь
 * {@code /api/v1/trades/selection/{steamId}} или {@code .../selection/{steamId}/items}.
 */
public class TradesOwnerJwtFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern SELECTION_PATH = Pattern.compile(
            "^/api/v1/trades/selection/(765[0-9]{14})(/items)?$");

    private final JwtSupportService jwtSupportService;

    public TradesOwnerJwtFilter(JwtSupportService jwtSupportService) {
        this.jwtSupportService = jwtSupportService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        var path = requestPath(request);
        if (path.startsWith("/actuator")) {
            return true;
        }
        return !requiresOwnerAuth(request.getMethod(), path);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        var path = requestPath(request);
        var pathSteamId = extractPathSteamId(path);
        var authHeader = request.getHeader(AUTHORIZATION);
        var token = extractBearerToken(authHeader);
        if (token == null || !jwtSupportService.isValid(token)) {
            writeUnauthorized(response);
            return;
        }
        var subjectSteamId = jwtSupportService.parseSubject(token);
        if (!subjectSteamId.equals(pathSteamId)) {
            writeForbidden(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean requiresOwnerAuth(String method, String path) {
        if (!"PUT".equals(method) && !"DELETE".equals(method)) {
            return false;
        }
        var m = SELECTION_PATH.matcher(path);
        if (!m.matches()) {
            return false;
        }
        if ("PUT".equals(method)) {
            return m.group(2) == null;
        }
        return true;
    }

    private static String extractPathSteamId(String path) {
        var m = SELECTION_PATH.matcher(path);
        if (!m.matches()) {
            return "";
        }
        return m.group(1);
    }

    private static String requestPath(HttpServletRequest request) {
        var uri = request.getRequestURI();
        var ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        var q = uri.indexOf('?');
        if (q >= 0) {
            uri = uri.substring(0, q);
        }
        return uri;
    }

    private static String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        var token = authHeader.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"detail\":\"Missing or invalid token\"}");
    }

    private static void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Forbidden\",\"detail\":\"Token subject does not match path steamId\"}");
    }
}
