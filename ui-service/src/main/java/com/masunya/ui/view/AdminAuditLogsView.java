package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.ui.client.OrderClient;
import com.masunya.ui.client.ServiceRequestClient;
import com.masunya.ui.client.TariffClient;
import com.masunya.ui.dto.AdminAuditLogResponse;
import com.masunya.ui.security.SessionState;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Route(value = "admin/logs", layout = MainLayout.class)
@PageTitle("Логи админа")
public class AdminAuditLogsView extends VerticalLayout implements BeforeEnterObserver {
    private final OrderClient orderClient;
    private final ServiceRequestClient serviceRequestClient;
    private final TariffClient tariffClient;

    private final Grid<AuditRow> grid = new Grid<>(AuditRow.class, false);
    private final IntegerField limitField = new IntegerField("Лимит из каждого сервиса");

    public AdminAuditLogsView(
            OrderClient orderClient,
            ServiceRequestClient serviceRequestClient,
            TariffClient tariffClient
    ) {
        this.orderClient = orderClient;
        this.serviceRequestClient = serviceRequestClient;
        this.tariffClient = tariffClient;

        H2 title = new H2("Логи действий администратора");
        limitField.setMin(1);
        limitField.setMax(500);
        limitField.setStepButtonsVisible(true);
        limitField.setValue(100);

        Button refreshButton = new Button("Обновить", e -> load());
        HorizontalLayout controls = new HorizontalLayout(limitField, refreshButton);

        grid.addColumn(AuditRow::getSource).setHeader("Сервис").setAutoWidth(true);
        grid.addColumn(AuditRow::getCreatedAt).setHeader("Время").setAutoWidth(true);
        grid.addColumn(AuditRow::getAdminUserId).setHeader("Админ").setAutoWidth(true);
        grid.addColumn(AuditRow::getAction).setHeader("Действие").setAutoWidth(true);
        grid.addColumn(AuditRow::getEntityType).setHeader("Сущность").setAutoWidth(true);
        grid.addColumn(AuditRow::getEntityId).setHeader("ID сущности").setAutoWidth(true);
        grid.addColumn(AuditRow::getDetails).setHeader("Детали").setAutoWidth(true).setFlexGrow(1);
        grid.setSizeFull();

        add(title, controls, grid);
        setSizeFull();
        load();
    }

    private void load() {
        try {
            String token = SessionState.getToken().orElseThrow();
            int limit = limitField.getValue() == null ? 100 : limitField.getValue();
            int normalizedLimit = Math.max(1, Math.min(limit, 500));

            List<AuditRow> rows = new ArrayList<>();
            rows.addAll(toRows("order-service", orderClient.getAdminAuditLogs(token, normalizedLimit)));
            rows.addAll(toRows("service-request-service", serviceRequestClient.getAdminAuditLogs(token, normalizedLimit)));
            rows.addAll(toRows("tariff-service", tariffClient.getAdminAuditLogs(token, normalizedLimit)));
            rows.sort(Comparator.comparing(AuditRow::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            grid.setItems(rows);
        } catch (Exception e) {
            Notification.show("Не удалось загрузить логи");
        }
    }

    private List<AuditRow> toRows(String source, List<AdminAuditLogResponse> logs) {
        if (logs == null) {
            return List.of();
        }
        return logs.stream()
                .map(log -> new AuditRow(
                        source,
                        log.getCreatedAt(),
                        log.getAdminUserId(),
                        log.getAction(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getDetails()
                ))
                .toList();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuards.requireRole(event, Role.ADMIN);
    }

    public static class AuditRow {
        private final String source;
        private final Instant createdAt;
        private final UUID adminUserId;
        private final String action;
        private final String entityType;
        private final UUID entityId;
        private final String details;

        public AuditRow(
                String source,
                Instant createdAt,
                UUID adminUserId,
                String action,
                String entityType,
                UUID entityId,
                String details
        ) {
            this.source = source;
            this.createdAt = createdAt;
            this.adminUserId = adminUserId;
            this.action = action;
            this.entityType = entityType;
            this.entityId = entityId;
            this.details = details;
        }

        public String getSource() {
            return source;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public UUID getAdminUserId() {
            return adminUserId;
        }

        public String getAction() {
            return action;
        }

        public String getEntityType() {
            return entityType;
        }

        public UUID getEntityId() {
            return entityId;
        }

        public String getDetails() {
            return details;
        }
    }
}
