package com.masunya.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ClientTariffAssignRequest {
    @NotNull
    private UUID userId;

    @NotNull
    private UUID tariffId;
}
