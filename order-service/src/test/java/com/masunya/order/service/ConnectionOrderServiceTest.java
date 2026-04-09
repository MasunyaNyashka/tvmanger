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
class ConnectionOrderServiceTest {

    @Mock
    private ConnectionOrderRepository orderRepository;

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @Mock
    private ClientTariffService clientTariffService;

    @InjectMocks
    private ConnectionOrderService connectionOrderService;

    @Test
    void createShouldTrimInputAndPersistSubmittedOrder() {
        UUID userId = UUID.randomUUID();
        UUID tariffId = UUID.randomUUID();
        ConnectionOrderCreateRequest request = new ConnectionOrderCreateRequest();
        request.setTariffId(tariffId);
        request.setFullName("  Ivan Ivanov  ");
        request.setAddress("  Moscow, Lenina 1  ");
        request.setPhone("  +79991234567  ");

        when(orderRepository.save(any(ConnectionOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConnectionOrderResponse response = connectionOrderService.create(userId, request);

        ArgumentCaptor<ConnectionOrder> captor = ArgumentCaptor.forClass(ConnectionOrder.class);
        verify(orderRepository).save(captor.capture());
        ConnectionOrder saved = captor.getValue();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getTariffId()).isEqualTo(tariffId);
        assertThat(saved.getFullName()).isEqualTo("Ivan Ivanov");
        assertThat(saved.getAddress()).isEqualTo("Moscow, Lenina 1");
        assertThat(saved.getPhone()).isEqualTo("+79991234567");
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.SUBMITTED);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getStatus()).isEqualTo(OrderStatus.SUBMITTED);
    }

    @Test
    void getMyOrderShouldReturnOrderForOwner() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        ConnectionOrder order = buildOrder(orderId, userId, OrderStatus.SUBMITTED);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        ConnectionOrderResponse response = connectionOrderService.getMyOrder(userId, orderId);

        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.SUBMITTED);
    }

    @Test
    void getMyOrderShouldRejectMissingOrder() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionOrderService.getMyOrder(userId, orderId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Order not found");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
                });
    }

    @Test
    void getAllForAdminShouldUseStatusAndDateRangeFilters() {
        ConnectionOrder order = buildOrder(UUID.randomUUID(), UUID.randomUUID(), OrderStatus.ACTIVE);
        LocalDate dateFrom = LocalDate.of(2026, 4, 1);
        LocalDate dateTo = LocalDate.of(2026, 4, 3);
        Instant expectedFrom = dateFrom.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant expectedTo = dateTo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);

        when(orderRepository.findAllByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                OrderStatus.ACTIVE,
                expectedFrom,
                expectedTo
        )).thenReturn(List.of(order));

        List<ConnectionOrderResponse> response = connectionOrderService.getAllForAdmin(
                OrderStatus.ACTIVE,
                dateFrom,
                dateTo
        );

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getId()).isEqualTo(order.getId());
        verify(orderRepository).findAllByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                OrderStatus.ACTIVE,
                expectedFrom,
                expectedTo
        );
    }

    @Test
    void updateStatusShouldActivateOrderCreateClientTariffAndWriteAudit() {
        UUID adminUserId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID tariffId = UUID.randomUUID();
        ConnectionOrder order = buildOrder(orderId, userId, OrderStatus.IN_PROGRESS);
        order.setTariffId(tariffId);

        ConnectionOrderStatusUpdateRequest request = new ConnectionOrderStatusUpdateRequest();
        request.setStatus(OrderStatus.ACTIVE);
        request.setAdminComment("Connected successfully");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(ConnectionOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConnectionOrderResponse response = connectionOrderService.updateStatus(adminUserId, orderId, request);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.ACTIVE);
        assertThat(response.getAdminComment()).isEqualTo("Connected successfully");
        verify(clientTariffService).createFromActivatedOrder(userId, tariffId);

        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(auditCaptor.capture());
        AdminAuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getAdminUserId()).isEqualTo(adminUserId);
        assertThat(auditLog.getAction()).isEqualTo("ORDER_STATUS_CHANGED");
        assertThat(auditLog.getEntityType()).isEqualTo("ORDER");
        assertThat(auditLog.getEntityId()).isEqualTo(orderId);
        assertThat(auditLog.getDetails()).isEqualTo("IN_PROGRESS -> ACTIVE");
    }

    @Test
    void updateStatusShouldNotCreateClientTariffWhenOrderAlreadyActive() {
        UUID adminUserId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        ConnectionOrder order = buildOrder(orderId, UUID.randomUUID(), OrderStatus.ACTIVE);

        ConnectionOrderStatusUpdateRequest request = new ConnectionOrderStatusUpdateRequest();
        request.setStatus(OrderStatus.ACTIVE);
        request.setAdminComment("No change");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(ConnectionOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        connectionOrderService.updateStatus(adminUserId, orderId, request);

        verify(clientTariffService, never()).createFromActivatedOrder(any(UUID.class), any(UUID.class));
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    void updateStatusShouldRejectInvalidTransition() {
        UUID orderId = UUID.randomUUID();
        ConnectionOrder order = buildOrder(orderId, UUID.randomUUID(), OrderStatus.SUBMITTED);

        ConnectionOrderStatusUpdateRequest request = new ConnectionOrderStatusUpdateRequest();
        request.setStatus(OrderStatus.ACTIVE);
        request.setAdminComment("Skip processing");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> connectionOrderService.updateStatus(UUID.randomUUID(), orderId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Invalid status transition");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                });

        verify(orderRepository, never()).save(any(ConnectionOrder.class));
        verify(clientTariffService, never()).createFromActivatedOrder(any(UUID.class), any(UUID.class));
        verify(adminAuditLogRepository, never()).save(any(AdminAuditLog.class));
    }

    @Test
    void updateStatusShouldRejectMissingOrder() {
        UUID orderId = UUID.randomUUID();
        ConnectionOrderStatusUpdateRequest request = new ConnectionOrderStatusUpdateRequest();
        request.setStatus(OrderStatus.CLOSED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionOrderService.updateStatus(UUID.randomUUID(), orderId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Order not found");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
                });

        verify(orderRepository, never()).save(any(ConnectionOrder.class));
        verify(adminAuditLogRepository, never()).save(any(AdminAuditLog.class));
    }

    @Test
    void getAllForAdminShouldUseUnfilteredQueryWhenNoArgumentsProvided() {
        ConnectionOrder order = buildOrder(UUID.randomUUID(), UUID.randomUUID(), OrderStatus.SUBMITTED);

        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));

        List<ConnectionOrderResponse> response = connectionOrderService.getAllForAdmin(null, null, null);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getId()).isEqualTo(order.getId());
        verify(orderRepository).findAllByOrderByCreatedAtDesc();
    }

    private ConnectionOrder buildOrder(UUID orderId, UUID userId, OrderStatus status) {
        ConnectionOrder order = new ConnectionOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setTariffId(UUID.randomUUID());
        order.setFullName("Ivan Ivanov");
        order.setAddress("Moscow, Lenina 1");
        order.setPhone("+79991234567");
        order.setStatus(status);
        order.setCreatedAt(Instant.parse("2026-04-01T10:15:30Z"));
        order.setUpdatedAt(Instant.parse("2026-04-01T10:15:30Z"));
        return order;
    }
}
