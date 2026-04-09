package com.masunya.tariff.service;

import com.masunya.common.enumerate.ConnectionType;
import com.masunya.common.exception.BusinessException;
import com.masunya.tariff.audit.AdminAuditLog;
import com.masunya.tariff.audit.AdminAuditLogRepository;
import com.masunya.tariff.dto.TariffCreateRequest;
import com.masunya.tariff.dto.TariffResponse;
import com.masunya.tariff.dto.TariffUpdateRequest;
import com.masunya.tariff.entity.Tariff;
import com.masunya.tariff.repository.TariffRepository;
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
class TariffServiceTest {

    @Mock
    private TariffRepository tariffRepository;

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @InjectMocks
    private TariffService tariffService;

    @Test
    void getPublicListShouldReturnOnlyMappedNonArchivedTariffs() {
        Tariff tariff = buildTariff(UUID.randomUUID(), "Base", false);

        when(tariffRepository.findAllByArchivedFalseOrderByNameAsc()).thenReturn(List.of(tariff));

        List<TariffResponse> response = tariffService.getPublicList();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getId()).isEqualTo(tariff.getId());
        assertThat(response.getFirst().getName()).isEqualTo("Base");
        assertThat(response.getFirst().isArchived()).isFalse();
    }

    @Test
    void getPublicByIdShouldReturnMappedTariff() {
        UUID tariffId = UUID.randomUUID();
        Tariff tariff = buildTariff(tariffId, "Premium", false);

        when(tariffRepository.findByIdAndArchivedFalse(tariffId)).thenReturn(Optional.of(tariff));

        TariffResponse response = tariffService.getPublicById(tariffId);

        assertThat(response.getId()).isEqualTo(tariffId);
        assertThat(response.getName()).isEqualTo("Premium");
    }

    @Test
    void getPublicByIdShouldRejectMissingTariff() {
        UUID tariffId = UUID.randomUUID();

        when(tariffRepository.findByIdAndArchivedFalse(tariffId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tariffService.getPublicById(tariffId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Tariff not found");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
                });
    }

    @Test
    void createShouldPersistTariffAndWriteAuditLog() {
        UUID adminUserId = UUID.randomUUID();
        TariffCreateRequest request = new TariffCreateRequest();
        request.setName("  Premium  ");
        request.setPrice(new BigDecimal("799.99"));
        request.setConnectionType(ConnectionType.CABLE);
        request.setDescription("Premium package");
        request.setChannels(List.of("HBO", "Discovery"));

        when(tariffRepository.existsByNameIgnoreCase("  Premium  ")).thenReturn(false);
        when(tariffRepository.save(any(Tariff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TariffResponse response = tariffService.create(adminUserId, request);

        ArgumentCaptor<Tariff> tariffCaptor = ArgumentCaptor.forClass(Tariff.class);
        verify(tariffRepository).save(tariffCaptor.capture());
        Tariff savedTariff = tariffCaptor.getValue();

        assertThat(savedTariff.getId()).isNotNull();
        assertThat(savedTariff.getName()).isEqualTo("Premium");
        assertThat(savedTariff.getPrice()).isEqualByComparingTo("799.99");
        assertThat(savedTariff.getConnectionType()).isEqualTo(ConnectionType.CABLE);
        assertThat(savedTariff.getDescription()).isEqualTo("Premium package");
        assertThat(savedTariff.getChannels()).containsExactly("HBO", "Discovery");
        assertThat(savedTariff.isArchived()).isFalse();
        assertThat(savedTariff.getCreatedAt()).isNotNull();

        assertThat(response.getName()).isEqualTo("Premium");
        assertThat(response.isArchived()).isFalse();

        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(auditCaptor.capture());
        AdminAuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getAdminUserId()).isEqualTo(adminUserId);
        assertThat(auditLog.getAction()).isEqualTo("TARIFF_CREATED");
        assertThat(auditLog.getEntityType()).isEqualTo("TARIFF");
        assertThat(auditLog.getEntityId()).isEqualTo(savedTariff.getId());
        assertThat(auditLog.getDetails()).isEqualTo("Premium");
    }

    @Test
    void createShouldRejectDuplicateName() {
        TariffCreateRequest request = new TariffCreateRequest();
        request.setName("Premium");
        request.setPrice(new BigDecimal("799.99"));
        request.setConnectionType(ConnectionType.CABLE);
        request.setChannels(List.of("HBO"));

        when(tariffRepository.existsByNameIgnoreCase("Premium")).thenReturn(true);

        assertThatThrownBy(() -> tariffService.create(UUID.randomUUID(), request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Tariff name already exists");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                });

        verify(tariffRepository, never()).save(any(Tariff.class));
        verify(adminAuditLogRepository, never()).save(any(AdminAuditLog.class));
    }

    @Test
    void updateShouldApplyChangesAndWriteAuditLog() {
        UUID adminUserId = UUID.randomUUID();
        UUID tariffId = UUID.randomUUID();
        Tariff tariff = buildTariff(tariffId, "Base", false);

        TariffUpdateRequest request = new TariffUpdateRequest();
        request.setName("  Premium  ");
        request.setPrice(new BigDecimal("899.99"));
        request.setConnectionType(ConnectionType.SATELLITE);
        request.setDescription("Updated package");
        request.setChannels(List.of("NatGeo", "Eurosport"));

        when(tariffRepository.findById(tariffId)).thenReturn(Optional.of(tariff));
        when(tariffRepository.existsByNameIgnoreCaseAndIdNot("  Premium  ", tariffId)).thenReturn(false);
        when(tariffRepository.save(any(Tariff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TariffResponse response = tariffService.update(adminUserId, tariffId, request);

        assertThat(response.getName()).isEqualTo("Premium");
        assertThat(response.getPrice()).isEqualByComparingTo("899.99");
        assertThat(response.getConnectionType()).isEqualTo(ConnectionType.SATELLITE);
        assertThat(response.getChannels()).containsExactly("NatGeo", "Eurosport");

        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(auditCaptor.capture());
        AdminAuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getAction()).isEqualTo("TARIFF_UPDATED");
        assertThat(auditLog.getEntityId()).isEqualTo(tariffId);
        assertThat(auditLog.getDetails()).isEqualTo("Premium");
    }

    @Test
    void updateShouldRejectMissingTariff() {
        UUID tariffId = UUID.randomUUID();
        TariffUpdateRequest request = new TariffUpdateRequest();
        request.setName("Premium");
        request.setPrice(new BigDecimal("899.99"));
        request.setConnectionType(ConnectionType.CABLE);
        request.setChannels(List.of("HBO"));

        when(tariffRepository.findById(tariffId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tariffService.update(UUID.randomUUID(), tariffId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Tariff not found");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
                });
    }

    @Test
    void updateShouldRejectDuplicateName() {
        UUID tariffId = UUID.randomUUID();
        Tariff tariff = buildTariff(tariffId, "Base", false);
        TariffUpdateRequest request = new TariffUpdateRequest();
        request.setName("Premium");
        request.setPrice(new BigDecimal("899.99"));
        request.setConnectionType(ConnectionType.CABLE);
        request.setChannels(List.of("HBO"));

        when(tariffRepository.findById(tariffId)).thenReturn(Optional.of(tariff));
        when(tariffRepository.existsByNameIgnoreCaseAndIdNot("Premium", tariffId)).thenReturn(true);

        assertThatThrownBy(() -> tariffService.update(UUID.randomUUID(), tariffId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Tariff name already exists");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                });

        verify(tariffRepository, never()).save(any(Tariff.class));
    }

    @Test
    void setArchivedShouldToggleFlagAndWriteAuditLog() {
        UUID adminUserId = UUID.randomUUID();
        UUID tariffId = UUID.randomUUID();
        Tariff tariff = buildTariff(tariffId, "Base", false);

        when(tariffRepository.findById(tariffId)).thenReturn(Optional.of(tariff));
        when(tariffRepository.save(any(Tariff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TariffResponse response = tariffService.setArchived(adminUserId, tariffId, true);

        assertThat(response.isArchived()).isTrue();

        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("TARIFF_ARCHIVED");
    }

    @Test
    void deleteShouldRemoveTariffAndWriteAuditLog() {
        UUID adminUserId = UUID.randomUUID();
        UUID tariffId = UUID.randomUUID();
        Tariff tariff = buildTariff(tariffId, "Base", false);

        when(tariffRepository.findById(tariffId)).thenReturn(Optional.of(tariff));

        tariffService.delete(adminUserId, tariffId);

        verify(tariffRepository).delete(tariff);

        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("TARIFF_DELETED");
        assertThat(auditCaptor.getValue().getEntityId()).isEqualTo(tariffId);
    }

    @Test
    void deleteShouldRejectMissingTariff() {
        UUID tariffId = UUID.randomUUID();

        when(tariffRepository.findById(tariffId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tariffService.delete(UUID.randomUUID(), tariffId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Tariff not found");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
                });

        verify(tariffRepository, never()).delete(any(Tariff.class));
        verify(adminAuditLogRepository, never()).save(any(AdminAuditLog.class));
    }

    private Tariff buildTariff(UUID id, String name, boolean archived) {
        Tariff tariff = new Tariff();
        tariff.setId(id);
        tariff.setName(name);
        tariff.setPrice(new BigDecimal("499.99"));
        tariff.setConnectionType(ConnectionType.CABLE);
        tariff.setDescription("Base package");
        tariff.setChannels(List.of("News", "Sport"));
        tariff.setArchived(archived);
        tariff.setCreatedAt(Instant.parse("2026-04-01T10:15:30Z"));
        return tariff;
    }
}
