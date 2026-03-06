package com.masunya.ui.dto;

import com.masunya.common.enumerate.ConnectionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class TariffCreateRequest {
    private String name;
    private BigDecimal price;
    private ConnectionType connectionType;
    private String description;
    private List<String> channels;
}
