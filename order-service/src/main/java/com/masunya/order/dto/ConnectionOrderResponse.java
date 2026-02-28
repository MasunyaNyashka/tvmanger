package com.masunya.order.dto;

import com.masunya.common.enumerate.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
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
