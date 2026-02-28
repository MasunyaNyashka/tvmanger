package com.masunya.servicerequest.entity;

import com.masunya.common.enumerate.ServiceRequestStatus;
import com.masunya.common.enumerate.ServiceRequestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "service_requests")
public class ServiceRequest {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceRequestType type;

    @Column(name = "tariff_id")
    private UUID tariffId;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 1000)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceRequestStatus status;

    @Column(name = "admin_comment", length = 1000)
    private String adminComment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
