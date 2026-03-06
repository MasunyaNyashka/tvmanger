package com.masunya.servicerequest.dto;

import com.masunya.servicerequest.audit.AdminAuditLog;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogResponse(
        UUID id,
        Instant createdAt,
        UUID adminUserId,
        String action,
        String entityType,
        UUID entityId,
        String details
) {
    public static AdminAuditLogResponse from(AdminAuditLog log) {
        return new AdminAuditLogResponse(
                log.getId(),
                log.getCreatedAt(),
                log.getAdminUserId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails()
        );
    }
}
