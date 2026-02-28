package com.masunya.ui.dto;

import com.masunya.common.enumerate.ServiceRequestStatus;
import com.masunya.common.enumerate.ServiceRequestType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ServiceRequestResponse {
    private UUID id;
    private UUID userId;
    private ServiceRequestType type;
    private UUID tariffId;
    private String address;
    private String phone;
    private String details;
    private ServiceRequestStatus status;
    private String adminComment;
    private Instant createdAt;
    private Instant updatedAt;
}
