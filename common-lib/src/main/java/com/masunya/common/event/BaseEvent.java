package com.masunya.common.event;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Getter
@Setter
public abstract class BaseEvent {
    private UUID eventId = UUID.randomUUID();
    private Instant createdAt = Instant.now();
}
