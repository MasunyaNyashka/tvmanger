package com.masunya.common.event;

import com.masunya.common.enumerate.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Getter
@Setter
public class OrderCreatedEvent extends BaseEvent {
    private UUID orderId;
    private UUID userId;
    private UUID tariffId;
    private OrderStatus status;
}
