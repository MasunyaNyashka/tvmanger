package com.masunya.ui.client;

import com.masunya.ui.dto.AdminAuditLogResponse;
import com.masunya.ui.dto.AuthResponse;
import com.masunya.ui.dto.LoginRequest;
import com.masunya.ui.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class AuthClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AuthClient(RestTemplate restTemplate, @Value("${services.auth}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public AuthResponse login(LoginRequest request) {
        // UI вызывает backend-логин и получает JWT.
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request);
        ResponseEntity<AuthResponse> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                entity,
                AuthResponse.class
        );
        return response.getBody();
    }

    public AuthResponse register(RegisterRequest request) {
        // UI вызывает backend-регистрацию и получает JWT.
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(request);
        ResponseEntity<AuthResponse> response = restTemplate.exchange(
                baseUrl + "/auth/register",
                HttpMethod.POST,
                entity,
                AuthResponse.class
        );
        return response.getBody();
    }

    public List<AdminAuditLogResponse> getAdminAuditLogs(String token, int limit) {
        // Для админ-экрана лога передаем bearer token и лимит выборки.
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<List<AdminAuditLogResponse>> response = restTemplate.exchange(
                baseUrl + "/admin/audit-logs?limit=" + limit,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }
}
