package com.masunya.servicerequest.dto;

import com.masunya.common.enumerate.ServiceRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceRequestStatusUpdateRequest {
    @NotNull
    private ServiceRequestStatus status;

    @Size(max = 1000)
    private String adminComment;
}
