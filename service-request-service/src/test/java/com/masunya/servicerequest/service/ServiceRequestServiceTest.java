package com.masunya.servicerequest.service;

import com.masunya.common.enumerate.ServiceRequestStatus;
import com.masunya.common.enumerate.ServiceRequestType;
import com.masunya.common.exception.BusinessException;
import com.masunya.servicerequest.audit.AdminAuditLog;
import com.masunya.servicerequest.audit.AdminAuditLogRepository;
import com.masunya.servicerequest.dto.ServiceRequestCreateRequest;
import com.masunya.servicerequest.dto.ServiceRequestResponse;
import com.masunya.servicerequest.dto.ServiceRequestStatusUpdateRequest;
import com.masunya.servicerequest.entity.ServiceRequest;
import com.masunya.servicerequest.repository.ServiceRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
class ServiceRequestServiceTest {

    @Mock
    private ServiceRequestRepository requestRepository;

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @InjectMocks
    private ServiceRequestService serviceRequestService;

    @Test
    void createShouldTrimInputAndPersistSubmittedRequest() {
        UUID userId = UUID.randomUUID();
        UUID tariffId = UUID.randomUUID();
        ServiceRequestCreateRequest request = new ServiceRequestCreateRequest();
        request.setType(ServiceRequestType.REPAIR);
        request.setTariffId(tariffId);
        request.setAddress("  Moscow, Lenina 1  ");
        request.setPhone("  +79991234567  ");
        request.setDetails("Internet is unstable");

        when(requestRepository.save(any(ServiceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequestResponse response = serviceRequestService.create(userId, request);

        ArgumentCaptor<ServiceRequest> captor = ArgumentCaptor.forClass(ServiceRequest.class);
        verify(requestRepository).save(captor.capture());
        ServiceRequest saved = captor.getValue();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getType()).isEqualTo(ServiceRequestType.REPAIR);
        assertThat(saved.getTariffId()).isEqualTo(tariffId);
        assertThat(saved.getAddress()).isEqualTo("Moscow, Lenina 1");
        assertThat(saved.getPhone()).isEqualTo("+79991234567");
        assertThat(saved.getDetails()).isEqualTo("Internet is unstable");
        assertThat(saved.getStatus()).isEqualTo(ServiceRequestStatus.SUBMITTED);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getStatus()).isEqualTo(ServiceRequestStatus.SUBMITTED);
    }

    @Test
    void getMyRequestShouldReturnOwnedRequest() {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        ServiceRequest request = buildRequest(requestId, userId, ServiceRequestStatus.SUBMITTED, ServiceRequestType.REPAIR);

        when(requestRepository.findByIdAndUserId(requestId, userId)).thenReturn(Optional.of(request));

        ServiceRequestResponse response = serviceRequestService.getMyRequest(userId, requestId);

        assertThat(response.getId()).isEqualTo(requestId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStatus()).isEqualTo(ServiceRequestStatus.SUBMITTED);
    }

    @Test
    void getMyRequestShouldRejectMissingRequest() {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        when(requestRepository.findByIdAndUserId(requestId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceRequestService.getMyRequest(userId, requestId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Service request not found");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
                });
    }

    @Test
    void getAllForAdminShouldUseStatusTypeAndDateRangeFilters() {
        ServiceRequest request = buildRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ServiceRequestStatus.IN_PROGRESS,
                ServiceRequestType.CHANGE_PLAN
        );
        LocalDate dateFrom = LocalDate.of(2026, 4, 1);
        LocalDate dateTo = LocalDate.of(2026, 4, 3);
        Instant expectedFrom = dateFrom.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant expectedTo = dateTo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);

        when(requestRepository.findAllByStatusAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                ServiceRequestStatus.IN_PROGRESS,
                ServiceRequestType.CHANGE_PLAN,
                expectedFrom,
                expectedTo
        )).thenReturn(List.of(request));

        List<ServiceRequestResponse> response = serviceRequestService.getAllForAdmin(
                ServiceRequestStatus.IN_PROGRESS,
                ServiceRequestType.CHANGE_PLAN,
                dateFrom,
                dateTo
        );

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getId()).isEqualTo(request.getId());
        verify(requestRepository).findAllByStatusAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                ServiceRequestStatus.IN_PROGRESS,
                ServiceRequestType.CHANGE_PLAN,
                expectedFrom,
                expectedTo
        );
    }

    @Test
    void getAllForAdminShouldUseUnfilteredQueryWhenNoArgumentsProvided() {
        ServiceRequest request = buildRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ServiceRequestStatus.SUBMITTED,
                ServiceRequestType.REPAIR
        );

        when(requestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(request));

        List<ServiceRequestResponse> response = serviceRequestService.getAllForAdmin(null, null, null, null);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getId()).isEqualTo(request.getId());
        verify(requestRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void updateStatusShouldApplyValidTransitionAndWriteAuditLog() {
        UUID adminUserId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        ServiceRequest serviceRequest = buildRequest(
                requestId,
                UUID.randomUUID(),
                ServiceRequestStatus.SUBMITTED,
                ServiceRequestType.REPAIR
        );

        ServiceRequestStatusUpdateRequest request = new ServiceRequestStatusUpdateRequest();
        request.setStatus(ServiceRequestStatus.IN_PROGRESS);
        request.setAdminComment("Technician assigned");

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequestResponse response = serviceRequestService.updateStatus(adminUserId, requestId, request);

        assertThat(response.getStatus()).isEqualTo(ServiceRequestStatus.IN_PROGRESS);
        assertThat(response.getAdminComment()).isEqualTo("Technician assigned");

        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(auditCaptor.capture());
        AdminAuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getAdminUserId()).isEqualTo(adminUserId);
        assertThat(auditLog.getAction()).isEqualTo("SERVICE_REQUEST_STATUS_CHANGED");
        assertThat(auditLog.getEntityType()).isEqualTo("SERVICE_REQUEST");
        assertThat(auditLog.getEntityId()).isEqualTo(requestId);
        assertThat(auditLog.getDetails()).isEqualTo("SUBMITTED -> IN_PROGRESS");
    }

    @Test
    void updateStatusShouldAllowSameStatus() {
        UUID adminUserId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        ServiceRequest serviceRequest = buildRequest(
                requestId,
                UUID.randomUUID(),
                ServiceRequestStatus.CLOSED,
                ServiceRequestType.REPAIR
        );

        ServiceRequestStatusUpdateRequest request = new ServiceRequestStatusUpdateRequest();
        request.setStatus(ServiceRequestStatus.CLOSED);
        request.setAdminComment("Already closed");

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequestResponse response = serviceRequestService.updateStatus(adminUserId, requestId, request);

        assertThat(response.getStatus()).isEqualTo(ServiceRequestStatus.CLOSED);
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    void updateStatusShouldRejectInvalidTransition() {
        UUID requestId = UUID.randomUUID();
        ServiceRequest serviceRequest = buildRequest(
                requestId,
                UUID.randomUUID(),
                ServiceRequestStatus.CLOSED,
                ServiceRequestType.REPAIR
        );

        ServiceRequestStatusUpdateRequest request = new ServiceRequestStatusUpdateRequest();
        request.setStatus(ServiceRequestStatus.IN_PROGRESS);
        request.setAdminComment("Reopen");

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(serviceRequest));

        assertThatThrownBy(() -> serviceRequestService.updateStatus(UUID.randomUUID(), requestId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Invalid status transition");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                });

        verify(requestRepository, never()).save(any(ServiceRequest.class));
        verify(adminAuditLogRepository, never()).save(any(AdminAuditLog.class));
    }

    @Test
    void updateStatusShouldRejectMissingRequest() {
        UUID requestId = UUID.randomUUID();
        ServiceRequestStatusUpdateRequest request = new ServiceRequestStatusUpdateRequest();
        request.setStatus(ServiceRequestStatus.CLOSED);

        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceRequestService.updateStatus(UUID.randomUUID(), requestId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Service request not found");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
                });

        verify(requestRepository, never()).save(any(ServiceRequest.class));
        verify(adminAuditLogRepository, never()).save(any(AdminAuditLog.class));
    }

    private ServiceRequest buildRequest(
            UUID requestId,
            UUID userId,
            ServiceRequestStatus status,
            ServiceRequestType type
    ) {
        ServiceRequest request = new ServiceRequest();
        request.setId(requestId);
        request.setUserId(userId);
        request.setType(type);
        request.setTariffId(UUID.randomUUID());
        request.setAddress("Moscow, Lenina 1");
        request.setPhone("+79991234567");
        request.setDetails("Default details");
        request.setStatus(status);
        request.setCreatedAt(Instant.parse("2026-04-01T10:15:30Z"));
        request.setUpdatedAt(Instant.parse("2026-04-01T10:15:30Z"));
        return request;
    }
}
