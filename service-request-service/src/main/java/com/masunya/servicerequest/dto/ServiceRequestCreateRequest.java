package com.masunya.servicerequest.dto;

import com.masunya.common.enumerate.ServiceRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ServiceRequestCreateRequest {
    @NotNull
    private ServiceRequestType type;

    private UUID tariffId;

    @NotBlank
    @Size(max = 300)
    private String address;

    @NotBlank
    @Pattern(regexp = "^\\+?\\d{10,15}$", message = "Invalid phone number")
    private String phone;

    @Size(max = 1000)
    private String details;
}
