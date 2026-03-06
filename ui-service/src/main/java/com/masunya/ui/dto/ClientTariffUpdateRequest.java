package com.masunya.ui.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ClientTariffUpdateRequest {
    private UUID tariffId;
    private BigDecimal customPrice;
    private String customConditions;
}
