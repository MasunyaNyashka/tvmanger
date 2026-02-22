package com.masunya.common.event;

import com.masunya.common.enumerate.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Getter
@Setter
public class OrderStatusChangedEvent extends BaseEvent {
    private UUID orderId;
    private OrderStatus oldStatus;
    private OrderStatus newStatus;
}
