package com.masunya.tariff.controller;

import com.masunya.tariff.audit.AdminAuditLogRepository;
import com.masunya.tariff.dto.AdminAuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {
    private final AdminAuditLogRepository adminAuditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminAuditLogResponse>> getLogs(
            @RequestParam(name = "limit", defaultValue = "200") int limit
    ) {
        int normalizedLimit = Math.max(1, Math.min(limit, 1000));
        List<AdminAuditLogResponse> response = adminAuditLogRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, normalizedLimit))
                .stream()
                .map(AdminAuditLogResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
