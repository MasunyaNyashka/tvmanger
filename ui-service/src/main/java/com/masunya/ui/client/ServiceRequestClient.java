package com.masunya.ui.client;

import com.masunya.common.enumerate.ServiceRequestStatus;
import com.masunya.common.enumerate.ServiceRequestType;
import com.masunya.ui.dto.AdminAuditLogResponse;
import com.masunya.ui.dto.ServiceRequestCreateRequest;
import com.masunya.ui.dto.ServiceRequestResponse;
import com.masunya.ui.dto.ServiceRequestStatusUpdateRequest;
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
public class ServiceRequestClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ServiceRequestClient(RestTemplate restTemplate, @Value("${services.serviceRequest}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public ServiceRequestResponse create(String token, ServiceRequestCreateRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<ServiceRequestCreateRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<ServiceRequestResponse> response = restTemplate.exchange(
                baseUrl + "/service-requests",
                HttpMethod.POST,
                entity,
                ServiceRequestResponse.class
        );
        return response.getBody();
    }

    public List<ServiceRequestResponse> getMyRequests(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<List<ServiceRequestResponse>> response = restTemplate.exchange(
                baseUrl + "/service-requests/my",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public ServiceRequestResponse getMyRequest(String token, UUID id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<ServiceRequestResponse> response = restTemplate.exchange(
                baseUrl + "/service-requests/my/" + id,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ServiceRequestResponse.class
        );
        return response.getBody();
    }

    public List<ServiceRequestResponse> getAllForAdmin(
            String token,
            ServiceRequestStatus status,
            ServiceRequestType type,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        StringBuilder url = new StringBuilder(baseUrl + "/service-requests/admin");
        boolean first = true;
        if (status != null) {
            url.append(first ? "?" : "&").append("status=").append(status);
            first = false;
        }
        if (type != null) {
            url.append(first ? "?" : "&").append("type=").append(type);
            first = false;
        }
        if (dateFrom != null) {
            url.append(first ? "?" : "&").append("dateFrom=").append(dateFrom);
            first = false;
        }
        if (dateTo != null) {
            url.append(first ? "?" : "&").append("dateTo=").append(dateTo);
        }
        ResponseEntity<List<ServiceRequestResponse>> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public ServiceRequestResponse updateStatus(String token, UUID id, ServiceRequestStatusUpdateRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<ServiceRequestStatusUpdateRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<ServiceRequestResponse> response = restTemplate.exchange(
                baseUrl + "/service-requests/" + id + "/status",
                HttpMethod.PATCH,
                entity,
                ServiceRequestResponse.class
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
