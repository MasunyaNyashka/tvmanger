package com.masunya.ui.dto;

import com.masunya.common.enumerate.ConnectionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class TariffResponse {
    private UUID id;
    private String name;
    private BigDecimal price;
    private ConnectionType connectionType;
    private String description;
    private List<String> channels;
    private boolean archived;
}
