package com.masunya.ui.dto;

import com.masunya.common.enumerate.ServiceRequestStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceRequestStatusUpdateRequest {
    private ServiceRequestStatus status;
    private String adminComment;
}
