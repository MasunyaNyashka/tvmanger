package com.masunya.functional;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class AuthApiFunctionalTest extends BaseApiFunctionalTest {

    @Test
    void clientShouldRegisterAndLogin() {
        String username = "client_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String password = "secret123";

        String registerToken = given()
                .spec(authSpec)
                .body(Map.of("username", username, "password", password))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");

        String loginToken = given()
                .spec(authSpec)
                .body(Map.of("username", username, "password", password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");

        assertThat(registerToken).isNotBlank();
        assertThat(loginToken).isNotBlank();
    }

    @Test
    void loginShouldRejectInvalidPassword() {
        String username = "client_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        given()
                .spec(authSpec)
                .body(Map.of("username", username, "password", "secret123"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(200);

        String responseBody = given()
                .spec(authSpec)
                .body(Map.of("username", username, "password", "wrong-password"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401)
                .extract()
                .asString();
    }
}
