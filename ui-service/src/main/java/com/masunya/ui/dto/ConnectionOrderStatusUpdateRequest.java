package com.masunya.ui.dto;

import com.masunya.common.enumerate.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConnectionOrderStatusUpdateRequest {
    private OrderStatus status;
    private String adminComment;
}
