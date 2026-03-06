package com.masunya.ui.client;

import com.masunya.ui.dto.AdminAuditLogResponse;
import com.masunya.ui.dto.TariffResponse;
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
public class TariffClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public TariffClient(RestTemplate restTemplate, @Value("${services.tariff}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<TariffResponse> getPublicTariffs() {
        ResponseEntity<List<TariffResponse>> response = restTemplate.exchange(
                baseUrl + "/tariffs",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public List<TariffResponse> getAllForAdmin(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<List<TariffResponse>> response = restTemplate.exchange(
                baseUrl + "/tariffs/admin",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public List<AdminAuditLogResponse> getAdminAuditLogs(String token, int limit) {
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
