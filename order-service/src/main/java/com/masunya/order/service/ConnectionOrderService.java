package com.masunya.order.service;

import com.masunya.common.enumerate.OrderStatus;
import com.masunya.common.exception.BusinessException;
import com.masunya.order.audit.AdminAuditLog;
import com.masunya.order.audit.AdminAuditLogRepository;
import com.masunya.order.dto.ConnectionOrderCreateRequest;
import com.masunya.order.dto.ConnectionOrderResponse;
import com.masunya.order.dto.ConnectionOrderStatusUpdateRequest;
import com.masunya.order.entity.ConnectionOrder;
import com.masunya.order.repository.ConnectionOrderRepository;
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
public class ConnectionOrderService {
    private final ConnectionOrderRepository orderRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final ClientTariffService clientTariffService;

    @Transactional
    public ConnectionOrderResponse create(UUID userId, ConnectionOrderCreateRequest request) {
        ConnectionOrder order = new ConnectionOrder();
        order.setId(UUID.randomUUID());
        order.setUserId(userId);
        order.setTariffId(request.getTariffId());
        order.setFullName(request.getFullName().trim());
        order.setAddress(request.getAddress().trim());
        order.setPhone(request.getPhone().trim());
        order.setStatus(OrderStatus.SUBMITTED);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<ConnectionOrderResponse> getMyOrders(UUID userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConnectionOrderResponse getMyOrder(UUID userId, UUID orderId) {
        ConnectionOrder order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<ConnectionOrderResponse> getAllForAdmin(
            OrderStatus status,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        List<ConnectionOrder> orders;
        if (dateFrom != null || dateTo != null) {
            LocalDate from = dateFrom != null ? dateFrom : dateTo;
            LocalDate to = dateTo != null ? dateTo : dateFrom;
            Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
            if (status != null) {
                orders = orderRepository.findAllByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                        status,
                        fromInstant,
                        toInstant
                );
            } else {
                orders = orderRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(
                        fromInstant,
                        toInstant
                );
            }
        } else if (status != null) {
            orders = orderRepository.findAllByStatusOrderByCreatedAtDesc(status);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        }
        return orders.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ConnectionOrderResponse updateStatus(UUID adminUserId, UUID orderId, ConnectionOrderStatusUpdateRequest request) {
        ConnectionOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND));
        OrderStatus oldStatus = order.getStatus();
        if (!isTransitionAllowed(order.getStatus(), request.getStatus())) {
            throw new BusinessException("Invalid status transition", HttpStatus.CONFLICT);
        }
        order.setStatus(request.getStatus());
        order.setAdminComment(request.getAdminComment());
        order.setUpdatedAt(Instant.now());
        ConnectionOrder saved = orderRepository.save(order);
        if (oldStatus != OrderStatus.ACTIVE && saved.getStatus() == OrderStatus.ACTIVE) {
            clientTariffService.createFromActivatedOrder(saved.getUserId(), saved.getTariffId());
        }
        saveAdminAudit(
                adminUserId,
                "ORDER_STATUS_CHANGED",
                "ORDER",
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

    private boolean isTransitionAllowed(OrderStatus current, OrderStatus next) {
        if (current == next) {
            return true;
        }
        return switch (current) {
            case SUBMITTED -> next == OrderStatus.IN_PROGRESS || next == OrderStatus.CLOSED;
            case IN_PROGRESS -> next == OrderStatus.ACTIVE || next == OrderStatus.SUSPENDED || next == OrderStatus.CLOSED;
            case ACTIVE -> next == OrderStatus.SUSPENDED || next == OrderStatus.CLOSED;
            case SUSPENDED -> next == OrderStatus.IN_PROGRESS || next == OrderStatus.CLOSED;
            case CLOSED -> false;
        };
    }

    private ConnectionOrderResponse toResponse(ConnectionOrder order) {
        return ConnectionOrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .tariffId(order.getTariffId())
                .fullName(order.getFullName())
                .address(order.getAddress())
                .phone(order.getPhone())
                .status(order.getStatus())
                .adminComment(order.getAdminComment())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
