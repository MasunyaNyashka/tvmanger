package com.masunya.tariff.dto;

import com.masunya.common.enumerate.ConnectionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class TariffResponse {
    private UUID id;
    private String name;
    private BigDecimal price;
    private ConnectionType connectionType;
    private String description;
    private List<String> channels;
    private boolean archived;
}
