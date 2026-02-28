package com.masunya.ui.dto;

import com.masunya.common.enumerate.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ConnectionOrderResponse {
    private UUID id;
    private UUID userId;
    private UUID tariffId;
    private String fullName;
    private String address;
    private String phone;
    private OrderStatus status;
    private String adminComment;
    private Instant createdAt;
    private Instant updatedAt;
}
