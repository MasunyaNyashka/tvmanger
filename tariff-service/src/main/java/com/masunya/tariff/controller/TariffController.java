package com.masunya.tariff.controller;

import com.masunya.common.exception.BusinessException;
import com.masunya.tariff.dto.TariffCreateRequest;
import com.masunya.tariff.dto.TariffResponse;
import com.masunya.tariff.dto.TariffUpdateRequest;
import com.masunya.tariff.service.TariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tariffs")
@RequiredArgsConstructor
public class TariffController {
    private final TariffService tariffService;

    @GetMapping
    public ResponseEntity<List<TariffResponse>> getPublicList() {
        return ResponseEntity.ok(tariffService.getPublicList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TariffResponse> getPublicById(@PathVariable UUID id) {
        return ResponseEntity.ok(tariffService.getPublicById(id));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TariffResponse>> getAllForAdmin() {
        return ResponseEntity.ok(tariffService.getAllForAdmin());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TariffCreateRequest request
    ) {
        return ResponseEntity.ok(tariffService.create(extractUserId(jwt), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody TariffUpdateRequest request
    ) {
        return ResponseEntity.ok(tariffService.update(extractUserId(jwt), id, request));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffResponse> archive(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(tariffService.setArchived(extractUserId(jwt), id, true));
    }

    @PatchMapping("/{id}/unarchive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffResponse> unarchive(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(tariffService.setArchived(extractUserId(jwt), id, false));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        tariffService.delete(extractUserId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(Jwt jwt) {
        try {
            // В subject JWT ожидаем UUID пользователя.
            return UUID.fromString(jwt.getSubject());
        } catch (Exception e) {
            throw new BusinessException("Invalid user id in token", HttpStatus.UNAUTHORIZED);
        }
    }
}
