package com.masunya.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.masunya.auth.audit.AdminAuditLogRepository;
import com.masunya.auth.entity.AuthUser;
import com.masunya.auth.repository.AuthUserRepository;
import com.masunya.common.enumerate.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_test_db")
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
        registry.add("jwt.expiration", () -> "7200");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerShouldPersistClientUserAndReturnJwt() throws Exception {
        String username = "user_" + UUID.randomUUID();
        String password = "secret123";

        String responseBody = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        assertThat(json.get("token").asText()).isNotBlank();

        AuthUser savedUser = authUserRepository.findByUsername(username).orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(Role.CLIENT);
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(passwordEncoder.matches(password, savedUser.getPasswordHash())).isTrue();
    }

    @Test
    void loginShouldReturnJwtAndCreateAdminAuditLogForAdmin() throws Exception {
        String username = "admin_" + UUID.randomUUID();
        String password = "secret123";
        saveUser(username, password, Role.ADMIN);
        long beforeCount = adminAuditLogRepository.count();

        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        assertThat(json.get("token").asText()).isNotBlank();
        assertThat(adminAuditLogRepository.count()).isEqualTo(beforeCount + 1);
        assertThat(adminAuditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 1))
                .getContent()
                .getFirst()
                .getAction()).isEqualTo("ADMIN_LOGIN");
    }

    @Test
    void adminAuditLogsEndpointShouldRequireAdminJwt() throws Exception {
        String username = "admin_" + UUID.randomUUID();
        String password = "secret123";
        saveUser(username, password, Role.ADMIN);

        mockMvc.perform(get("/admin/audit-logs"))
                .andExpect(status().isUnauthorized());

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        mockMvc.perform(get("/admin/audit-logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private void saveUser(String username, String password, Role role) {
        AuthUser user = new AuthUser();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        authUserRepository.save(user);
    }
}
