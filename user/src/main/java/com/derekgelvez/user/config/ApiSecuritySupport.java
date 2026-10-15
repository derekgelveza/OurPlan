package com.derekgelvez.user.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Security settings shared by the user app and the calendar app, so both answer
 * unauthenticated/forbidden requests and CORS the same way.
 *
 * <p>Errors use the same JSON shape as the calendar's {@code ErrorResponseDTO}:
 * {@code {"code": "...", "message": "...", "details": {}}}.
 */
public final class ApiSecuritySupport {

    private ApiSecuritySupport() {
    }

    /** 401 when a protected endpoint is called without a valid Bearer token. */
    public static AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHORIZED", "Authentication required: send a valid Bearer token");
    }

    /** 403 when an authenticated user is not allowed to do something. */
    public static AccessDeniedHandler forbiddenHandler() {
        return (request, response, accessDeniedException) -> writeError(response, HttpServletResponse.SC_FORBIDDEN,
                "FORBIDDEN", "You do not have permission to do this");
    }

    /**
     * CORS only matters for browsers (e.g. Expo web). Native React Native requests don't send an
     * Origin header and are not affected. Tokens travel in the Authorization header, never cookies,
     * so credentials are not allowed.
     */
    public static CorsConfigurationSource corsConfigurationSource(String[] allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins).map(String::trim).filter(o -> !o.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /** Writes an error body from a filter, where @RestControllerAdvice handlers don't run. */
    public static void writeError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"" + escape(code) + "\",\"message\":\"" + escape(message)
                + "\",\"details\":{}}");
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
