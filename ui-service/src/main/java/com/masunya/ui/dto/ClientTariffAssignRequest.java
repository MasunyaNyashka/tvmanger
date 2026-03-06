package com.masunya.ui.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ClientTariffAssignRequest {
    private UUID userId;
    private UUID tariffId;
}
