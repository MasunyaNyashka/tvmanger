package com.masunya.tariff.dto;

import com.masunya.common.enumerate.ConnectionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class TariffUpdateRequest {
    @NotBlank
    @Size(max = 150)
    private String name;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    @NotNull
    private ConnectionType connectionType;

    @Size(max = 1000)
    private String description;

    @NotEmpty
    @Size(max = 200)
    private List<@NotBlank @Size(max = 100) String> channels;
}
