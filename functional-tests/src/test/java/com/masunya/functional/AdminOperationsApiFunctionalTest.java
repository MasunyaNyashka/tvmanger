package com.masunya.functional;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class AdminOperationsApiFunctionalTest extends BaseApiFunctionalTest {

    @Test
    void adminShouldViewOrdersAndChangeOrderStatus() {
        FunctionalAuthSupport.AuthSession client = FunctionalAuthSupport.registerAndLoginClient();
        String adminToken = FunctionalAuthSupport.adminToken(UUID.randomUUID());

        String tariffId = given()
                .spec(tariffSpec)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of(
                        "name", "Admin Order Tariff " + UUID.randomUUID(),
                        "price", 599.99,
                        "connectionType", "CABLE",
                        "description", "Tariff for admin order flow",
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

        List<String> adminOrderIds = given()
                .spec(orderSpec)
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/orders/admin")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("id");

        assertThat(adminOrderIds).contains(orderId);

        given()
                .spec(orderSpec)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("status", "IN_PROGRESS", "adminComment", "Assigned"))
                .when()
                .patch("/orders/{id}/status", orderId)
                .then()
                .statusCode(200);

        String updatedStatus = given()
                .spec(orderSpec)
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/orders/admin")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("find { it.id == '%s' }.status".formatted(orderId));

        assertThat(updatedStatus).isEqualTo("IN_PROGRESS");
    }

    @Test
    void adminShouldCreateUpdateAndArchiveTariffAndSeeAuditLog() {
        String adminToken = FunctionalAuthSupport.adminToken(UUID.randomUUID());
        String tariffName = "Admin Managed Tariff " + UUID.randomUUID();

        String tariffId = given()
                .spec(tariffSpec)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of(
                        "name", tariffName,
                        "price", 499.99,
                        "connectionType", "CABLE",
                        "description", "Initial tariff",
                        "channels", List.of("News", "Sport")
                ))
                .when()
                .post("/tariffs")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("id");

        given()
                .spec(tariffSpec)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of(
                        "name", tariffName + " Updated",
                        "price", 699.99,
                        "connectionType", "SATELLITE",
                        "description", "Updated tariff",
                        "channels", List.of("NatGeo", "Eurosport")
                ))
                .when()
                .put("/tariffs/{id}", tariffId)
                .then()
                .statusCode(200);

        given()
                .spec(tariffSpec)
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .patch("/tariffs/{id}/archive", tariffId)
                .then()
                .statusCode(200);

        boolean presentInPublicList = given()
                .spec(tariffSpec)
                .when()
                .get("/tariffs")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("id", String.class)
                .contains(tariffId);

        assertThat(presentInPublicList).isFalse();

        List<String> auditActions = given()
                .spec(tariffSpec)
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("limit", 20)
                .when()
                .get("/admin/audit-logs")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("action");

        assertThat(auditActions).contains("TARIFF_CREATED", "TARIFF_UPDATED", "TARIFF_ARCHIVED");
    }

    @Test
    void adminShouldViewServiceRequestsAndChangeStatusAndSeeAuditLog() {
        FunctionalAuthSupport.AuthSession client = FunctionalAuthSupport.registerAndLoginClient();
        String adminToken = FunctionalAuthSupport.adminToken(UUID.randomUUID());

        String requestId = given()
                .spec(serviceRequestSpec)
                .header("Authorization", "Bearer " + client.token())
                .body(Map.of(
                        "type", "REPAIR",
                        "address", "Moscow, Arbat 10",
                        "phone", "+79991234567",
                        "details", "No signal"
                ))
                .when()
                .post("/service-requests")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("id");

        List<String> requestIds = given()
                .spec(serviceRequestSpec)
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/service-requests/admin")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("id");

        assertThat(requestIds).contains(requestId);

        given()
                .spec(serviceRequestSpec)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("status", "IN_PROGRESS", "adminComment", "Accepted"))
                .when()
                .patch("/service-requests/{id}/status", requestId)
                .then()
                .statusCode(200);

        List<String> auditActions = given()
                .spec(serviceRequestSpec)
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("limit", 20)
                .when()
                .get("/admin/audit-logs")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("action");

        assertThat(auditActions).contains("SERVICE_REQUEST_STATUS_CHANGED");
    }
}
