package com.masunya.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ConnectionOrderCreateRequest {
    @NotNull
    private UUID tariffId;

    @NotBlank
    @Size(max = 200)
    private String fullName;

    @NotBlank
    @Size(max = 300)
    private String address;

    @NotBlank
    @Pattern(regexp = "^\\+?\\d{10,15}$", message = "Invalid phone number")
    private String phone;
}
