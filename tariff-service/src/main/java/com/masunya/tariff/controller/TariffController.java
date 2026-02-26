package com.masunya.tariff.controller;

import com.masunya.tariff.dto.TariffCreateRequest;
import com.masunya.tariff.dto.TariffResponse;
import com.masunya.tariff.dto.TariffUpdateRequest;
import com.masunya.tariff.service.TariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<TariffResponse> create(@Valid @RequestBody TariffCreateRequest request) {
        return ResponseEntity.ok(tariffService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TariffUpdateRequest request
    ) {
        return ResponseEntity.ok(tariffService.update(id, request));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffResponse> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(tariffService.setArchived(id, true));
    }

    @PatchMapping("/{id}/unarchive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffResponse> unarchive(@PathVariable UUID id) {
        return ResponseEntity.ok(tariffService.setArchived(id, false));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tariffService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
