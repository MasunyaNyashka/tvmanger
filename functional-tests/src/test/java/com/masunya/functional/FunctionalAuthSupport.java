package com.masunya.functional;

import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

final class FunctionalAuthSupport {

    private FunctionalAuthSupport() {
    }

    static AuthSession registerAndLoginClient() {
        String username = "client_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String password = "secret123";

        given()
                .spec(BaseApiFunctionalTest.authSpec)
                .body(Map.of("username", username, "password", password))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(200);

        String token = given()
                .spec(BaseApiFunctionalTest.authSpec)
                .body(Map.of("username", username, "password", password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");

        return new AuthSession(username, password, token);
    }

    static String adminToken(UUID adminUserId) {
        SecretKey key = new SecretKeySpec(
                BaseApiFunctionalTest.JWT_SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(adminUserId.toString())
                .claim("username", "functional-admin")
                .claim("role", "ADMIN")
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plusSeconds(7200)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    record AuthSession(String username, String password, String token) {
    }
}
