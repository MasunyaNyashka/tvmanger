package com.masunya.ui.security;

import com.masunya.common.enumerate.Role;
import com.vaadin.flow.server.VaadinSession;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SessionState {
    private static final String TOKEN_KEY = "token";
    private static final String ROLE_KEY = "role";
    private static final String USER_ID_KEY = "userId";
    private static final String USERNAME_KEY = "username";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SessionState() {
    }

    public static void setToken(String token) {
        VaadinSession.getCurrent().setAttribute(TOKEN_KEY, token);
        parseAndStoreClaims(token);
    }

    public static Optional<String> getToken() {
        return Optional.ofNullable((String) VaadinSession.getCurrent().getAttribute(TOKEN_KEY));
    }

    public static void clear() {
        VaadinSession.getCurrent().setAttribute(TOKEN_KEY, null);
        VaadinSession.getCurrent().setAttribute(ROLE_KEY, null);
        VaadinSession.getCurrent().setAttribute(USER_ID_KEY, null);
        VaadinSession.getCurrent().setAttribute(USERNAME_KEY, null);
    }

    public static Optional<Role> getRole() {
        return Optional.ofNullable((Role) VaadinSession.getCurrent().getAttribute(ROLE_KEY));
    }

    public static Optional<UUID> getUserId() {
        return Optional.ofNullable((UUID) VaadinSession.getCurrent().getAttribute(USER_ID_KEY));
    }

    public static Optional<String> getUsername() {
        return Optional.ofNullable((String) VaadinSession.getCurrent().getAttribute(USERNAME_KEY));
    }

    public static boolean isAuthenticated() {
        return getToken().isPresent();
    }

    public static boolean hasRole(Role role) {
        return getRole().map(r -> r == role).orElse(false);
    }

    private static void parseAndStoreClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return;
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = MAPPER.readValue(payloadJson, new TypeReference<>() {});
            Object role = claims.get("role");
            Object sub = claims.get("sub");
            Object username = claims.get("username");
            if (role instanceof String r) {
                VaadinSession.getCurrent().setAttribute(ROLE_KEY, Role.valueOf(r));
            }
            if (sub instanceof String s) {
                VaadinSession.getCurrent().setAttribute(USER_ID_KEY, UUID.fromString(s));
            }
            if (username instanceof String u) {
                VaadinSession.getCurrent().setAttribute(USERNAME_KEY, u);
            }
        } catch (Exception ignored) {
        }
    }
}
