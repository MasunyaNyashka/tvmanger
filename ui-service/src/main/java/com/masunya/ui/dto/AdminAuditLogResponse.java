package com.masunya.ui.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class AdminAuditLogResponse {
    private UUID id;
    private Instant createdAt;
    private UUID adminUserId;
    private String action;
    private String entityType;
    private UUID entityId;
    private String details;
}
