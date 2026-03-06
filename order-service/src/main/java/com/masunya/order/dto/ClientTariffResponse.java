package com.masunya.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ClientTariffResponse {
    private UUID id;
    private UUID userId;
    private UUID tariffId;
    private BigDecimal customPrice;
    private String customConditions;
    private Instant createdAt;
    private Instant updatedAt;
}
