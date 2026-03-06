package com.masunya.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "client_tariffs")
public class ClientTariff {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tariff_id", nullable = false)
    private UUID tariffId;

    @Column(name = "custom_price", precision = 12, scale = 2)
    private BigDecimal customPrice;

    @Column(name = "custom_conditions", length = 1000)
    private String customConditions;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
