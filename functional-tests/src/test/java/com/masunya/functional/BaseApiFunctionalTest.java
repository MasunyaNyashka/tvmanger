package com.masunya.functional;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;

abstract class BaseApiFunctionalTest {

    protected static final String AUTH_BASE_URL = System.getProperty("auth.baseUrl", "http://localhost:18081");
    protected static final String TARIFF_BASE_URL = System.getProperty("tariff.baseUrl", "http://localhost:18082");
    protected static final String ORDER_BASE_URL = System.getProperty("order.baseUrl", "http://localhost:18083");
    protected static final String SERVICE_REQUEST_BASE_URL = System.getProperty(
            "serviceRequest.baseUrl",
            "http://localhost:18084"
    );
    protected static final String JWT_SECRET = System.getProperty(
            "jwt.secret",
            "super-secret-key-super-secret-key-super-secret-key"
    );

    protected static RequestSpecification authSpec;
    protected static RequestSpecification tariffSpec;
    protected static RequestSpecification orderSpec;
    protected static RequestSpecification serviceRequestSpec;

    @BeforeAll
    static void setUpSpec() {
        RestAssured.baseURI = AUTH_BASE_URL;
        authSpec = new RequestSpecBuilder()
                .setBaseUri(AUTH_BASE_URL)
                .setContentType(ContentType.JSON)
                .build();
        tariffSpec = new RequestSpecBuilder()
                .setBaseUri(TARIFF_BASE_URL)
                .setContentType(ContentType.JSON)
                .build();
        orderSpec = new RequestSpecBuilder()
                .setBaseUri(ORDER_BASE_URL)
                .setContentType(ContentType.JSON)
                .build();
        serviceRequestSpec = new RequestSpecBuilder()
                .setBaseUri(SERVICE_REQUEST_BASE_URL)
                .setContentType(ContentType.JSON)
                .build();
    }
}
