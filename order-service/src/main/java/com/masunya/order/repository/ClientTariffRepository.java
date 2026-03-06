package com.masunya.order.repository;

import com.masunya.order.entity.ClientTariff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClientTariffRepository extends JpaRepository<ClientTariff, UUID> {
    List<ClientTariff> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    List<ClientTariff> findAllByOrderByCreatedAtDesc();
}
