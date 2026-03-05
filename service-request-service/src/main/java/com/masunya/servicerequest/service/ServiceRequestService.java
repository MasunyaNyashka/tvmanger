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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    private final ServiceRequestRepository requestRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;

    @Transactional
    public ServiceRequestResponse create(UUID userId, ServiceRequestCreateRequest request) {
        ServiceRequest sr = new ServiceRequest();
        sr.setId(UUID.randomUUID());
        sr.setUserId(userId);
        sr.setType(request.getType());
        sr.setTariffId(request.getTariffId());
        sr.setAddress(request.getAddress().trim());
        sr.setPhone(request.getPhone().trim());
        sr.setDetails(request.getDetails());
        sr.setStatus(ServiceRequestStatus.SUBMITTED);
        sr.setCreatedAt(Instant.now());
        sr.setUpdatedAt(Instant.now());
        return toResponse(requestRepository.save(sr));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> getMyRequests(UUID userId) {
        return requestRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse getMyRequest(UUID userId, UUID id) {
        ServiceRequest sr = requestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException("Service request not found", HttpStatus.NOT_FOUND));
        return toResponse(sr);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> getAllForAdmin(
            ServiceRequestStatus status,
            ServiceRequestType type,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        List<ServiceRequest> requests;
        if (dateFrom != null || dateTo != null) {
            LocalDate from = dateFrom != null ? dateFrom : dateTo;
            LocalDate to = dateTo != null ? dateTo : dateFrom;
            Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
            if (status != null && type != null) {
                requests = requestRepository.findAllByStatusAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                        status,
                        type,
                        fromInstant,
                        toInstant
                );
            } else if (status != null) {
                requests = requestRepository.findAllByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                        status,
                        fromInstant,
                        toInstant
                );
            } else if (type != null) {
                requests = requestRepository.findAllByTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                        type,
                        fromInstant,
                        toInstant
                );
            } else {
                requests = requestRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(fromInstant, toInstant);
            }
        } else if (status != null && type != null) {
            requests = requestRepository.findAllByStatusAndTypeOrderByCreatedAtDesc(status, type);
        } else if (status != null) {
            requests = requestRepository.findAllByStatusOrderByCreatedAtDesc(status);
        } else if (type != null) {
            requests = requestRepository.findAllByTypeOrderByCreatedAtDesc(type);
        } else {
            requests = requestRepository.findAllByOrderByCreatedAtDesc();
        }
        return requests.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ServiceRequestResponse updateStatus(UUID adminUserId, UUID id, ServiceRequestStatusUpdateRequest request) {
        ServiceRequest sr = requestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Service request not found", HttpStatus.NOT_FOUND));
        ServiceRequestStatus oldStatus = sr.getStatus();
        if (!isTransitionAllowed(sr.getStatus(), request.getStatus())) {
            throw new BusinessException("Invalid status transition", HttpStatus.CONFLICT);
        }
        sr.setStatus(request.getStatus());
        sr.setAdminComment(request.getAdminComment());
        sr.setUpdatedAt(Instant.now());
        ServiceRequest saved = requestRepository.save(sr);
        saveAdminAudit(
                adminUserId,
                "SERVICE_REQUEST_STATUS_CHANGED",
                "SERVICE_REQUEST",
                saved.getId(),
                oldStatus + " -> " + saved.getStatus()
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

    private boolean isTransitionAllowed(ServiceRequestStatus current, ServiceRequestStatus next) {
        if (current == next) {
            return true;
        }
        return switch (current) {
            case SUBMITTED -> next == ServiceRequestStatus.IN_PROGRESS || next == ServiceRequestStatus.CLOSED;
            case IN_PROGRESS -> next == ServiceRequestStatus.CLOSED;
            case CLOSED -> false;
        };
    }

    private ServiceRequestResponse toResponse(ServiceRequest sr) {
        return ServiceRequestResponse.builder()
                .id(sr.getId())
                .userId(sr.getUserId())
                .type(sr.getType())
                .tariffId(sr.getTariffId())
                .address(sr.getAddress())
                .phone(sr.getPhone())
                .details(sr.getDetails())
                .status(sr.getStatus())
                .adminComment(sr.getAdminComment())
                .createdAt(sr.getCreatedAt())
                .updatedAt(sr.getUpdatedAt())
                .build();
    }
}
