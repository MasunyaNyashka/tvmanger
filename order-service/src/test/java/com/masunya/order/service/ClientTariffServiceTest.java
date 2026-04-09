package com.masunya.order.service;

import com.masunya.common.exception.BusinessException;
import com.masunya.order.audit.AdminAuditLog;
import com.masunya.order.audit.AdminAuditLogRepository;
import com.masunya.order.dto.ClientTariffAssignRequest;
import com.masunya.order.dto.ClientTariffResponse;
import com.masunya.order.dto.ClientTariffUpdateRequest;
import com.masunya.order.entity.ClientTariff;
import com.masunya.order.repository.ClientTariffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientTariffServiceTest {

    @Mock
    private ClientTariffRepository clientTariffRepository;

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @InjectMocks
    private ClientTariffService clientTariffService;

    @Test
    void assignShouldCreateClientTariffAndWriteAuditLog() {
        UUID adminUserId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID tariffId = UUID.randomUUID();
        ClientTariffAssignRequest request = new ClientTariffAssignRequest();
        request.setUserId(userId);
        request.setTariffId(tariffId);

        when(clientTariffRepository.save(any(ClientTariff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientTariffResponse response = clientTariffService.assign(adminUserId, request);

        ArgumentCaptor<ClientTariff> tariffCaptor = ArgumentCaptor.forClass(ClientTariff.class);
        verify(clientTariffRepository).save(tariffCaptor.capture());
        ClientTariff savedTariff = tariffCaptor.getValue();

        assertThat(savedTariff.getId()).isNotNull();
        assertThat(savedTariff.getUserId()).isEqualTo(userId);
        assertThat(savedTariff.getTariffId()).isEqualTo(tariffId);
        assertThat(savedTariff.getCustomPrice()).isNull();
        assertThat(savedTariff.getCustomConditions()).isNull();
        assertThat(savedTariff.getCreatedAt()).isNotNull();
        assertThat(savedTariff.getUpdatedAt()).isNotNull();

        assertThat(response.getId()).isEqualTo(savedTariff.getId());
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getTariffId()).isEqualTo(tariffId);

        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(auditCaptor.capture());
        AdminAuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getAdminUserId()).isEqualTo(adminUserId);
        assertThat(auditLog.getAction()).isEqualTo("CLIENT_TARIFF_ASSIGNED");
        assertThat(auditLog.getEntityType()).isEqualTo("CLIENT_TARIFF");
        assertThat(auditLog.getEntityId()).isEqualTo(savedTariff.getId());
        assertThat(auditLog.getDetails()).contains("userId=" + userId, "tariffId=" + tariffId);
    }

    @Test
    void getMyTariffsShouldReturnMappedItems() {
        UUID userId = UUID.randomUUID();
        ClientTariff tariff = buildClientTariff(UUID.randomUUID(), userId, UUID.randomUUID());

        when(clientTariffRepository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(tariff));

        List<ClientTariffResponse> response = clientTariffService.getMyTariffs(userId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getId()).isEqualTo(tariff.getId());
        assertThat(response.getFirst().getUserId()).isEqualTo(userId);
    }

    @Test
    void getForAdminShouldUseUserSpecificQueryWhenUserIdProvided() {
        UUID userId = UUID.randomUUID();
        ClientTariff tariff = buildClientTariff(UUID.randomUUID(), userId, UUID.randomUUID());

        when(clientTariffRepository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(tariff));

        List<ClientTariffResponse> response = clientTariffService.getForAdmin(userId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getUserId()).isEqualTo(userId);
        verify(clientTariffRepository).findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void getForAdminShouldUseGlobalQueryWhenUserIdMissing() {
        ClientTariff tariff = buildClientTariff(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        when(clientTariffRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(tariff));

        List<ClientTariffResponse> response = clientTariffService.getForAdmin(null);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getId()).isEqualTo(tariff.getId());
        verify(clientTariffRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void createFromActivatedOrderShouldPersistClientTariffWithoutAuditLog() {
        UUID userId = UUID.randomUUID();
        UUID tariffId = UUID.randomUUID();

        when(clientTariffRepository.save(any(ClientTariff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        clientTariffService.createFromActivatedOrder(userId, tariffId);

        ArgumentCaptor<ClientTariff> tariffCaptor = ArgumentCaptor.forClass(ClientTariff.class);
        verify(clientTariffRepository).save(tariffCaptor.capture());
        ClientTariff savedTariff = tariffCaptor.getValue();

        assertThat(savedTariff.getId()).isNotNull();
        assertThat(savedTariff.getUserId()).isEqualTo(userId);
        assertThat(savedTariff.getTariffId()).isEqualTo(tariffId);
        assertThat(savedTariff.getCustomPrice()).isNull();
        assertThat(savedTariff.getCustomConditions()).isNull();
        verify(adminAuditLogRepository, never()).save(any(AdminAuditLog.class));
    }

    @Test
    void updateTariffShouldApplyChangesAndWriteAuditLog() {
        UUID adminUserId = UUID.randomUUID();
        UUID clientTariffId = UUID.randomUUID();
        UUID oldTariffId = UUID.randomUUID();
        UUID newTariffId = UUID.randomUUID();
        ClientTariff clientTariff = buildClientTariff(clientTariffId, UUID.randomUUID(), oldTariffId);

        ClientTariffUpdateRequest request = new ClientTariffUpdateRequest();
        request.setTariffId(newTariffId);
        request.setCustomPrice(new BigDecimal("999.99"));
        request.setCustomConditions("Special support");

        when(clientTariffRepository.findById(clientTariffId)).thenReturn(Optional.of(clientTariff));
        when(clientTariffRepository.save(any(ClientTariff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientTariffResponse response = clientTariffService.updateTariff(adminUserId, clientTariffId, request);

        assertThat(response.getTariffId()).isEqualTo(newTariffId);
        assertThat(response.getCustomPrice()).isEqualByComparingTo("999.99");
        assertThat(response.getCustomConditions()).isEqualTo("Special support");

        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(auditCaptor.capture());
        AdminAuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getAdminUserId()).isEqualTo(adminUserId);
        assertThat(auditLog.getAction()).isEqualTo("CLIENT_TARIFF_CHANGED");
        assertThat(auditLog.getEntityType()).isEqualTo("CLIENT_TARIFF");
        assertThat(auditLog.getEntityId()).isEqualTo(clientTariffId);
        assertThat(auditLog.getDetails()).contains("oldTariffId=" + oldTariffId);
        assertThat(auditLog.getDetails()).contains("newTariffId=" + newTariffId);
        assertThat(auditLog.getDetails()).contains("customPrice=999.99");
    }

    @Test
    void updateTariffShouldRejectMissingClientTariff() {
        UUID clientTariffId = UUID.randomUUID();
        ClientTariffUpdateRequest request = new ClientTariffUpdateRequest();
        request.setTariffId(UUID.randomUUID());

        when(clientTariffRepository.findById(clientTariffId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientTariffService.updateTariff(UUID.randomUUID(), clientTariffId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Client tariff not found");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
                });

        verify(clientTariffRepository, never()).save(any(ClientTariff.class));
        verify(adminAuditLogRepository, never()).save(any(AdminAuditLog.class));
    }

    private ClientTariff buildClientTariff(UUID id, UUID userId, UUID tariffId) {
        ClientTariff clientTariff = new ClientTariff();
        clientTariff.setId(id);
        clientTariff.setUserId(userId);
        clientTariff.setTariffId(tariffId);
        clientTariff.setCustomPrice(new BigDecimal("499.00"));
        clientTariff.setCustomConditions("Default conditions");
        clientTariff.setCreatedAt(Instant.parse("2026-04-01T10:15:30Z"));
        clientTariff.setUpdatedAt(Instant.parse("2026-04-01T10:15:30Z"));
        return clientTariff;
    }
}
