package com.masunya.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masunya.order.audit.AdminAuditLogRepository;
import com.masunya.order.repository.ClientTariffRepository;
import com.masunya.order.repository.ConnectionOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("order_test_db")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("jwt.secret", () -> "super-secret-key-super-secret-key-super-secret-key");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConnectionOrderRepository connectionOrderRepository;

    @Autowired
    private ClientTariffRepository clientTariffRepository;

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    @Test
    void clientShouldCreateOrderAndSeeItInMyOrders() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tariffId = UUID.randomUUID();

        String createResponse = mockMvc.perform(post("/orders")
                        .with(clientJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tariffId": "%s",
                                  "fullName": "  Ivan Ivanov  ",
                                  "address": "  Moscow, Lenina 1  ",
                                  "phone": "+79991234567"
                                }
                                """.formatted(tariffId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String orderId = objectMapper.readTree(createResponse).get("id").asText();

        assertThat(connectionOrderRepository.findById(UUID.fromString(orderId))).isPresent();

        mockMvc.perform(get("/orders/my")
                        .with(clientJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId))
                .andExpect(jsonPath("$[0].fullName").value("Ivan Ivanov"))
                .andExpect(jsonPath("$[0].address").value("Moscow, Lenina 1"))
                .andExpect(jsonPath("$[0].phone").value("+79991234567"));
    }

    @Test
    void adminShouldActivateOrderCreateClientTariffAndWriteAuditLog() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        UUID tariffId = UUID.randomUUID();
        long beforeAuditCount = adminAuditLogRepository.count();

        String createResponse = mockMvc.perform(post("/orders")
                        .with(clientJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tariffId": "%s",
                                  "fullName": "Ivan Ivanov",
                                  "address": "Moscow, Lenina 1",
                                  "phone": "+79991234567"
                                }
                                """.formatted(tariffId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String orderId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(patch("/orders/{id}/status", orderId)
                        .with(adminJwt(adminUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS",
                                  "adminComment": "Processing"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/orders/{id}/status", orderId)
                        .with(adminJwt(adminUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACTIVE",
                                  "adminComment": "Connected"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(clientTariffRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .hasSize(1)
                .first()
                .satisfies(item -> assertThat(item.getTariffId()).isEqualTo(tariffId));

        assertThat(adminAuditLogRepository.count()).isEqualTo(beforeAuditCount + 2);
        assertThat(adminAuditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 1))
                .getContent()
                .getFirst()
                .getAction()).isEqualTo("ORDER_STATUS_CHANGED");
    }

    @Test
    void adminEndpointsShouldRequireAdminRole() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/orders/admin")
                        .with(clientJwt(userId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/client-tariffs/admin")
                        .with(clientJwt(userId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/orders/admin")
                        .with(adminJwt(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private RequestPostProcessor clientJwt(UUID userId) {
        return jwt()
                .jwt(jwt -> jwt.subject(userId.toString()).claim("role", "CLIENT"))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
    }

    private RequestPostProcessor adminJwt(UUID userId) {
        return jwt()
                .jwt(jwt -> jwt.subject(userId.toString()).claim("role", "ADMIN"))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
