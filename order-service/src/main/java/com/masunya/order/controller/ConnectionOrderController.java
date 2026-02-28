package com.masunya.order.controller;

import com.masunya.common.enumerate.OrderStatus;
import com.masunya.common.exception.BusinessException;
import com.masunya.order.dto.ConnectionOrderCreateRequest;
import com.masunya.order.dto.ConnectionOrderResponse;
import com.masunya.order.dto.ConnectionOrderStatusUpdateRequest;
import com.masunya.order.service.ConnectionOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class ConnectionOrderController {
    private final ConnectionOrderService orderService;

    @PostMapping
    public ResponseEntity<ConnectionOrderResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConnectionOrderCreateRequest request
    ) {
        return ResponseEntity.ok(orderService.create(extractUserId(jwt), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ConnectionOrderResponse>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(orderService.getMyOrders(extractUserId(jwt)));
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<ConnectionOrderResponse> getMyOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(orderService.getMyOrder(extractUserId(jwt), id));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ConnectionOrderResponse>> getAllForAdmin(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(orderService.getAllForAdmin(status, dateFrom, dateTo));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConnectionOrderResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ConnectionOrderStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }

    private UUID extractUserId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (Exception e) {
            throw new BusinessException("Invalid user id in token");
        }
    }
}
