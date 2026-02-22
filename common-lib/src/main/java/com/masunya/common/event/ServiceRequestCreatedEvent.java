package com.masunya.common.event;

import com.masunya.common.enumerate.ServiceRequestType;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Getter
@Setter
public class ServiceRequestCreatedEvent extends BaseEvent {
    private UUID requestId;
    private UUID userId;
    private ServiceRequestType type;
}
