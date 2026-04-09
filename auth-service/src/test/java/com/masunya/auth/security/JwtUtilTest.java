package com.masunya.auth.security;

import com.masunya.auth.config.JwtProperties;
import com.masunya.common.enumerate.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    @Test
    void generateTokenShouldIncludeExpectedClaims() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("super-secret-key-super-secret-key-super-secret-key");
        properties.setExpiration(3600);

        JwtUtil jwtUtil = new JwtUtil(properties);
        jwtUtil.init();

        UUID userId = UUID.randomUUID();

        String token = jwtUtil.generateToken(userId, "tester", Role.ADMIN);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("username", String.class)).isEqualTo("tester");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }
}
