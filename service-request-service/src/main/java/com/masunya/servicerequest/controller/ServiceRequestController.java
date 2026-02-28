package com.masunya.servicerequest.controller;

import com.masunya.common.enumerate.ServiceRequestStatus;
import com.masunya.common.enumerate.ServiceRequestType;
import com.masunya.common.exception.BusinessException;
import com.masunya.servicerequest.dto.ServiceRequestCreateRequest;
import com.masunya.servicerequest.dto.ServiceRequestResponse;
import com.masunya.servicerequest.dto.ServiceRequestStatusUpdateRequest;
import com.masunya.servicerequest.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {
    private final ServiceRequestService serviceRequestService;

    @PostMapping
    public ResponseEntity<ServiceRequestResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ServiceRequestCreateRequest request
    ) {
        return ResponseEntity.ok(serviceRequestService.create(extractUserId(jwt), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ServiceRequestResponse>> getMyRequests(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(serviceRequestService.getMyRequests(extractUserId(jwt)));
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<ServiceRequestResponse> getMyRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(serviceRequestService.getMyRequest(extractUserId(jwt), id));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceRequestResponse>> getAllForAdmin(
            @RequestParam(name = "status", required = false) ServiceRequestStatus status,
            @RequestParam(name = "type", required = false) ServiceRequestType type,
            @RequestParam(name = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(serviceRequestService.getAllForAdmin(status, type, dateFrom, dateTo));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceRequestResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ServiceRequestStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(serviceRequestService.updateStatus(id, request));
    }

    private UUID extractUserId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (Exception e) {
            throw new BusinessException("Invalid user id in token");
        }
    }
}
