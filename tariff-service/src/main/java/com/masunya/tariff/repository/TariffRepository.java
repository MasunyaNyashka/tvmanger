package com.masunya.tariff.repository;

import com.masunya.tariff.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TariffRepository extends JpaRepository<Tariff, UUID> {
    List<Tariff> findAllByArchivedFalseOrderByNameAsc();
    Optional<Tariff> findByIdAndArchivedFalse(UUID id);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
