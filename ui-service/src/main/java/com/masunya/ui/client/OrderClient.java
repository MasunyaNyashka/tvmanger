package com.masunya.ui.client;

import com.masunya.common.enumerate.OrderStatus;
import com.masunya.ui.dto.AdminAuditLogResponse;
import com.masunya.ui.dto.ClientTariffAssignRequest;
import com.masunya.ui.dto.ClientTariffResponse;
import com.masunya.ui.dto.ClientTariffUpdateRequest;
import com.masunya.ui.dto.ConnectionOrderCreateRequest;
import com.masunya.ui.dto.ConnectionOrderResponse;
import com.masunya.ui.dto.ConnectionOrderStatusUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class OrderClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrderClient(RestTemplate restTemplate, @Value("${services.order}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public ConnectionOrderResponse create(String token, ConnectionOrderCreateRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<ConnectionOrderCreateRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<ConnectionOrderResponse> response = restTemplate.exchange(
                baseUrl + "/orders",
                HttpMethod.POST,
                entity,
                ConnectionOrderResponse.class
        );
        return response.getBody();
    }

    public List<ConnectionOrderResponse> getMyOrders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<List<ConnectionOrderResponse>> response = restTemplate.exchange(
                baseUrl + "/orders/my",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public ConnectionOrderResponse getMyOrder(String token, UUID id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<ConnectionOrderResponse> response = restTemplate.exchange(
                baseUrl + "/orders/my/" + id,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ConnectionOrderResponse.class
        );
        return response.getBody();
    }

    public List<ConnectionOrderResponse> getAllForAdmin(
            String token,
            OrderStatus status,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        StringBuilder url = new StringBuilder(baseUrl + "/orders/admin");
        boolean first = true;
        if (status != null) {
            url.append(first ? "?" : "&").append("status=").append(status);
            first = false;
        }
        if (dateFrom != null) {
            url.append(first ? "?" : "&").append("dateFrom=").append(dateFrom);
            first = false;
        }
        if (dateTo != null) {
            url.append(first ? "?" : "&").append("dateTo=").append(dateTo);
        }
        ResponseEntity<List<ConnectionOrderResponse>> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public ConnectionOrderResponse updateStatus(String token, UUID id, ConnectionOrderStatusUpdateRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<ConnectionOrderStatusUpdateRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<ConnectionOrderResponse> response = restTemplate.exchange(
                baseUrl + "/orders/" + id + "/status",
                HttpMethod.PATCH,
                entity,
                ConnectionOrderResponse.class
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

    public ClientTariffResponse assignClientTariff(String token, ClientTariffAssignRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<ClientTariffAssignRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<ClientTariffResponse> response = restTemplate.exchange(
                baseUrl + "/client-tariffs/admin",
                HttpMethod.POST,
                entity,
                ClientTariffResponse.class
        );
        return response.getBody();
    }

    public List<ClientTariffResponse> getMyClientTariffs(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<List<ClientTariffResponse>> response = restTemplate.exchange(
                baseUrl + "/client-tariffs/my",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public List<ClientTariffResponse> getClientTariffsForAdmin(String token, UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        String url = userId == null
                ? baseUrl + "/client-tariffs/admin"
                : baseUrl + "/client-tariffs/admin?userId=" + userId;
        ResponseEntity<List<ClientTariffResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public ClientTariffResponse updateClientTariff(
            String token,
            UUID clientTariffId,
            ClientTariffUpdateRequest request
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<ClientTariffUpdateRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<ClientTariffResponse> response = restTemplate.exchange(
                baseUrl + "/client-tariffs/admin/" + clientTariffId + "/tariff",
                HttpMethod.PATCH,
                entity,
                ClientTariffResponse.class
        );
        return response.getBody();
    }
}
