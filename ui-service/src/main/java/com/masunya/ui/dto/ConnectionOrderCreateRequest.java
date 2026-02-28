package com.masunya.ui.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ConnectionOrderCreateRequest {
    private UUID tariffId;
    private String fullName;
    private String address;
    private String phone;
}
