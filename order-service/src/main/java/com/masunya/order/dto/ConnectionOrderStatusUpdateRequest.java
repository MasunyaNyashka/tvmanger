package com.masunya.order.dto;

import com.masunya.common.enumerate.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConnectionOrderStatusUpdateRequest {
    @NotNull
    private OrderStatus status;

    @Size(max = 1000)
    private String adminComment;
}
