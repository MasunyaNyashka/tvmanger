package com.masunya.tariff;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masunya.common.enumerate.ConnectionType;
import com.masunya.tariff.audit.AdminAuditLogRepository;
import com.masunya.tariff.repository.TariffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
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
class TariffServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tariff_test_db")
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
    private TariffRepository tariffRepository;

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    @Test
    void publicListShouldReturnOnlyNonArchivedTariffs() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        String archivedTariffId = createTariff(adminUserId, "Archive Public Hidden " + UUID.randomUUID());
        createTariff(adminUserId, "Visible Public " + UUID.randomUUID());

        mockMvc.perform(patch("/tariffs/{id}/archive", archivedTariffId)
                        .with(adminJwt(adminUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(get("/tariffs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").isArray())
                .andExpect(jsonPath("$[?(@.id=='" + archivedTariffId + "')]").isEmpty());
    }

    @Test
    void createShouldPersistTariffAndWriteAuditLog() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        long beforeAuditCount = adminAuditLogRepository.count();
        String tariffName = "Premium " + UUID.randomUUID();

        String responseBody = mockMvc.perform(post("/tariffs")
                        .with(adminJwt(adminUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "price": 799.99,
                                  "connectionType": "CABLE",
                                  "description": "Premium package",
                                  "channels": ["HBO", "Discovery"]
                                }
                                """.formatted(tariffName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(tariffName))
                .andExpect(jsonPath("$.price").value(799.99))
                .andExpect(jsonPath("$.connectionType").value("CABLE"))
                .andExpect(jsonPath("$.archived").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String tariffId = objectMapper.readTree(responseBody).get("id").asText();

        var savedTariff = tariffRepository.findById(UUID.fromString(tariffId)).orElseThrow();
        assertThat(savedTariff.getName()).isEqualTo(tariffName);
        assertThat(savedTariff.getPrice()).isEqualByComparingTo(new BigDecimal("799.99"));
        assertThat(savedTariff.getConnectionType()).isEqualTo(ConnectionType.CABLE);
        assertThat(savedTariff.getChannels()).containsExactly("HBO", "Discovery");
        assertThat(savedTariff.isArchived()).isFalse();

        assertThat(adminAuditLogRepository.count()).isEqualTo(beforeAuditCount + 1);
        assertThat(adminAuditLogRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent()
                .getFirst()
                .getAction()).isEqualTo("TARIFF_CREATED");
    }

    @Test
    void archiveShouldHideTariffFromPublicListButKeepItInAdminList() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        String tariffId = createTariff(adminUserId, "Archive me " + UUID.randomUUID());

        mockMvc.perform(patch("/tariffs/{id}/archive", tariffId)
                        .with(adminJwt(adminUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tariffId))
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(get("/tariffs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + tariffId + "')]").isEmpty());

        mockMvc.perform(get("/tariffs/admin")
                        .with(adminJwt(adminUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + tariffId + "' && @.archived==true)]").isNotEmpty());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt(UUID adminUserId) {
        return jwt()
                .jwt(jwt -> jwt.subject(adminUserId.toString()).claim("role", "ADMIN"))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private String createTariff(UUID adminUserId, String name) throws Exception {
        String responseBody = mockMvc.perform(post("/tariffs")
                        .with(adminJwt(adminUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "price": 499.99,
                                  "connectionType": "CABLE",
                                  "description": "Base package",
                                  "channels": ["News", "Sport"]
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody).get("id").asText();
    }
}
