package com.masunya.order.repository;

import com.masunya.common.enumerate.OrderStatus;
import com.masunya.order.entity.ConnectionOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectionOrderRepository extends JpaRepository<ConnectionOrder, UUID> {
    List<ConnectionOrder> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<ConnectionOrder> findByIdAndUserId(UUID id, UUID userId);

    List<ConnectionOrder> findAllByStatusOrderByCreatedAtDesc(OrderStatus status);
    List<ConnectionOrder> findAllByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
    List<ConnectionOrder> findAllByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            OrderStatus status,
            Instant from,
            Instant to
    );

    List<ConnectionOrder> findAllByOrderByCreatedAtDesc();
}
