package com.masunya.servicerequest.repository;

import com.masunya.common.enumerate.ServiceRequestStatus;
import com.masunya.common.enumerate.ServiceRequestType;
import com.masunya.servicerequest.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
    List<ServiceRequest> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<ServiceRequest> findByIdAndUserId(UUID id, UUID userId);

    List<ServiceRequest> findAllByStatusOrderByCreatedAtDesc(ServiceRequestStatus status);
    List<ServiceRequest> findAllByTypeOrderByCreatedAtDesc(ServiceRequestType type);
    List<ServiceRequest> findAllByStatusAndTypeOrderByCreatedAtDesc(
            ServiceRequestStatus status,
            ServiceRequestType type
    );
    List<ServiceRequest> findAllByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
    List<ServiceRequest> findAllByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            ServiceRequestStatus status,
            Instant from,
            Instant to
    );
    List<ServiceRequest> findAllByTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            ServiceRequestType type,
            Instant from,
            Instant to
    );
    List<ServiceRequest> findAllByStatusAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            ServiceRequestStatus status,
            ServiceRequestType type,
            Instant from,
            Instant to
    );
    List<ServiceRequest> findAllByOrderByCreatedAtDesc();
}
