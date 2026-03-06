package com.masunya.order.service;

import com.masunya.common.exception.BusinessException;
import com.masunya.order.audit.AdminAuditLog;
import com.masunya.order.audit.AdminAuditLogRepository;
import com.masunya.order.dto.ClientTariffAssignRequest;
import com.masunya.order.dto.ClientTariffResponse;
import com.masunya.order.dto.ClientTariffUpdateRequest;
import com.masunya.order.entity.ClientTariff;
import com.masunya.order.repository.ClientTariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientTariffService {
    private final ClientTariffRepository clientTariffRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;

    @Transactional
    public ClientTariffResponse assign(UUID adminUserId, ClientTariffAssignRequest request) {
        ClientTariff clientTariff = new ClientTariff();
        clientTariff.setId(UUID.randomUUID());
        clientTariff.setUserId(request.getUserId());
        clientTariff.setTariffId(request.getTariffId());
        clientTariff.setCustomPrice(null);
        clientTariff.setCustomConditions(null);
        clientTariff.setCreatedAt(Instant.now());
        clientTariff.setUpdatedAt(Instant.now());
        ClientTariff saved = clientTariffRepository.save(clientTariff);
        saveAdminAudit(
                adminUserId,
                "CLIENT_TARIFF_ASSIGNED",
                "CLIENT_TARIFF",
                saved.getId(),
                "userId=" + saved.getUserId() + ", tariffId=" + saved.getTariffId()
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ClientTariffResponse> getMyTariffs(UUID userId) {
        return clientTariffRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClientTariffResponse> getForAdmin(UUID userId) {
        List<ClientTariff> items = userId != null
                ? clientTariffRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                : clientTariffRepository.findAllByOrderByCreatedAtDesc();
        return items.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void createFromActivatedOrder(UUID userId, UUID tariffId) {
        ClientTariff clientTariff = new ClientTariff();
        clientTariff.setId(UUID.randomUUID());
        clientTariff.setUserId(userId);
        clientTariff.setTariffId(tariffId);
        clientTariff.setCustomPrice(null);
        clientTariff.setCustomConditions(null);
        clientTariff.setCreatedAt(Instant.now());
        clientTariff.setUpdatedAt(Instant.now());
        clientTariffRepository.save(clientTariff);
    }

    @Transactional
    public ClientTariffResponse updateTariff(UUID adminUserId, UUID clientTariffId, ClientTariffUpdateRequest request) {
        ClientTariff clientTariff = clientTariffRepository.findById(clientTariffId)
                .orElseThrow(() -> new BusinessException("Client tariff not found", HttpStatus.NOT_FOUND));
        UUID oldTariffId = clientTariff.getTariffId();
        clientTariff.setTariffId(request.getTariffId());
        clientTariff.setCustomPrice(request.getCustomPrice());
        clientTariff.setCustomConditions(request.getCustomConditions());
        clientTariff.setUpdatedAt(Instant.now());
        ClientTariff saved = clientTariffRepository.save(clientTariff);
        saveAdminAudit(
                adminUserId,
                "CLIENT_TARIFF_CHANGED",
                "CLIENT_TARIFF",
                saved.getId(),
                "oldTariffId=" + oldTariffId
                        + ", newTariffId=" + saved.getTariffId()
                        + ", customPrice=" + saved.getCustomPrice()
        );
        return toResponse(saved);
    }

    private void saveAdminAudit(UUID adminUserId, String action, String entityType, UUID entityId, String details) {
        AdminAuditLog log = new AdminAuditLog();
        log.setId(UUID.randomUUID());
        log.setCreatedAt(Instant.now());
        log.setAdminUserId(adminUserId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        adminAuditLogRepository.save(log);
    }

    private ClientTariffResponse toResponse(ClientTariff item) {
        return ClientTariffResponse.builder()
                .id(item.getId())
                .userId(item.getUserId())
                .tariffId(item.getTariffId())
                .customPrice(item.getCustomPrice())
                .customConditions(item.getCustomConditions())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
