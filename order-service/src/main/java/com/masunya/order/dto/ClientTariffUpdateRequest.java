package com.masunya.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ClientTariffUpdateRequest {
    @NotNull
    private UUID tariffId;

    private BigDecimal customPrice;

    @Size(max = 1000)
    private String customConditions;
}
