package com.masunya.functional;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class ServiceRequestFlowApiFunctionalTest extends BaseApiFunctionalTest {

    @Test
    void clientServiceRequestShouldBeVisibleToAdminAndChangeStatus() {
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

        List<String> adminIds = given()
                .spec(serviceRequestSpec)
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/service-requests/admin")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("id");

        assertThat(adminIds).contains(requestId);

        given()
                .spec(serviceRequestSpec)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("status", "IN_PROGRESS", "adminComment", "Accepted"))
                .when()
                .patch("/service-requests/{id}/status", requestId)
                .then()
                .statusCode(200);

        String status = given()
                .spec(serviceRequestSpec)
                .header("Authorization", "Bearer " + client.token())
                .when()
                .get("/service-requests/my/{id}", requestId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("status");

        assertThat(status).isEqualTo("IN_PROGRESS");
    }
}
