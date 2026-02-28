package com.masunya.ui.dto;

import com.masunya.common.enumerate.ServiceRequestType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ServiceRequestCreateRequest {
    private ServiceRequestType type;
    private UUID tariffId;
    private String address;
    private String phone;
    private String details;
}
