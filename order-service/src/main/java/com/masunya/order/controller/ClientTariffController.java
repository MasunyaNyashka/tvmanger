package com.masunya.order.controller;

import com.masunya.common.exception.BusinessException;
import com.masunya.order.dto.ClientTariffAssignRequest;
import com.masunya.order.dto.ClientTariffResponse;
import com.masunya.order.dto.ClientTariffUpdateRequest;
import com.masunya.order.service.ClientTariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/client-tariffs")
@RequiredArgsConstructor
public class ClientTariffController {
    private final ClientTariffService clientTariffService;

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientTariffResponse> assign(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ClientTariffAssignRequest request
    ) {
        return ResponseEntity.ok(clientTariffService.assign(extractUserId(jwt), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ClientTariffResponse>> getMy(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(clientTariffService.getMyTariffs(extractUserId(jwt)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClientTariffResponse>> getForAdmin(
            @RequestParam(name = "userId", required = false) UUID userId
    ) {
        return ResponseEntity.ok(clientTariffService.getForAdmin(userId));
    }

    @PatchMapping("/admin/{id}/tariff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientTariffResponse> updateTariff(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @Valid @RequestBody ClientTariffUpdateRequest request
    ) {
        return ResponseEntity.ok(clientTariffService.updateTariff(extractUserId(jwt), id, request));
    }

    private UUID extractUserId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (Exception e) {
            throw new BusinessException("Invalid user id in token");
        }
    }
}
