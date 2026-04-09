package com.masunya.functional;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class OrderFlowApiFunctionalTest extends BaseApiFunctionalTest {

    @Test
    void clientOrderShouldBecomeActiveAndCreateClientTariff() {
        FunctionalAuthSupport.AuthSession client = FunctionalAuthSupport.registerAndLoginClient();
        String adminToken = FunctionalAuthSupport.adminToken(UUID.randomUUID());

        String tariffId = given()
                .spec(tariffSpec)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of(
                        "name", "Functional Tariff " + UUID.randomUUID(),
                        "price", 499.99,
                        "connectionType", "CABLE",
                        "description", "Functional flow tariff",
                        "channels", List.of("HBO", "Discovery")
                ))
                .when()
                .post("/tariffs")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("id");

        String orderId = given()
                .spec(orderSpec)
                .header("Authorization", "Bearer " + client.token())
                .body(Map.of(
                        "tariffId", tariffId,
                        "fullName", "Ivan Ivanov",
                        "address", "Moscow, Lenina 1",
                        "phone", "+79991234567"
                ))
                .when()
                .post("/orders")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("id");

        given()
                .spec(orderSpec)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("status", "IN_PROGRESS", "adminComment", "Assigned"))
                .when()
                .patch("/orders/{id}/status", orderId)
                .then()
                .statusCode(200);

        given()
                .spec(orderSpec)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("status", "ACTIVE", "adminComment", "Connected"))
                .when()
                .patch("/orders/{id}/status", orderId)
                .then()
                .statusCode(200);

        List<String> tariffIds = given()
                .spec(orderSpec)
                .header("Authorization", "Bearer " + client.token())
                .when()
                .get("/client-tariffs/my")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("tariffId");

        assertThat(tariffIds).contains(tariffId);
    }
}
