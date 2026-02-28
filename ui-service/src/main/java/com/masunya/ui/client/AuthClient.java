package com.masunya.ui.client;

import com.masunya.ui.dto.AuthResponse;
import com.masunya.ui.dto.LoginRequest;
import com.masunya.ui.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AuthClient(RestTemplate restTemplate, @Value("${services.auth}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public AuthResponse login(LoginRequest request) {
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
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(request);
        ResponseEntity<AuthResponse> response = restTemplate.exchange(
                baseUrl + "/auth/register",
                HttpMethod.POST,
                entity,
                AuthResponse.class
        );
        return response.getBody();
    }
}
