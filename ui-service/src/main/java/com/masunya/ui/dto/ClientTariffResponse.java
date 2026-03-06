package com.masunya.ui.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ClientTariffResponse {
    private UUID id;
    private UUID userId;
    private UUID tariffId;
    private BigDecimal customPrice;
    private String customConditions;
    private Instant createdAt;
    private Instant updatedAt;
}
