package com.masunya.ui.view;

import com.masunya.common.enumerate.OrderStatus;
import com.masunya.common.enumerate.Role;
import com.masunya.ui.client.OrderClient;
import com.masunya.ui.dto.ConnectionOrderResponse;
import com.masunya.ui.dto.ConnectionOrderStatusUpdateRequest;
import com.masunya.ui.security.SessionState;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.UUID;

@Route(value = "admin/orders", layout = MainLayout.class)
@PageTitle("Заявки (админ)")
public class AdminOrdersView extends VerticalLayout implements BeforeEnterObserver {
    private final OrderClient orderClient;
    private final Grid<ConnectionOrderResponse> grid = new Grid<>(ConnectionOrderResponse.class, false);
    private final ComboBox<OrderStatus> statusFilter = new ComboBox<>("Статус");
    private final DatePicker dateFrom = new DatePicker("Дата с");
    private final DatePicker dateTo = new DatePicker("Дата по");

    public AdminOrdersView(OrderClient orderClient) {
        this.orderClient = orderClient;

        H2 title = new H2("Заявки (админ)");
        statusFilter.setItems(OrderStatus.values());

        Button refresh = new Button("Обновить", e -> load());
        HorizontalLayout filters = new HorizontalLayout(statusFilter, dateFrom, dateTo, refresh);

        grid.addColumn(ConnectionOrderResponse::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(ConnectionOrderResponse::getUserId).setHeader("User ID").setAutoWidth(true);
        grid.addColumn(ConnectionOrderResponse::getTariffId).setHeader("Tariff ID").setAutoWidth(true);
        grid.addColumn(ConnectionOrderResponse::getStatus).setHeader("Статус").setAutoWidth(true);
        grid.addColumn(ConnectionOrderResponse::getAdminComment).setHeader("Комментарий").setAutoWidth(true);
        grid.addColumn(ConnectionOrderResponse::getCreatedAt).setHeader("Создано").setAutoWidth(true);
        grid.addComponentColumn(order -> new Button("Сменить статус", e -> openStatusDialog(order.getId())))
                .setHeader("Действия");

        add(title, filters, grid);
        setSizeFull();
        load();
    }

    private void openStatusDialog(UUID orderId) {
        Dialog dialog = new Dialog();
        ComboBox<OrderStatus> status = new ComboBox<>("Новый статус");
        status.setItems(OrderStatus.values());
        TextArea comment = new TextArea("Комментарий");
        Button save = new Button("Сохранить", e -> {
            try {
                String token = SessionState.getToken().orElseThrow();
                ConnectionOrderStatusUpdateRequest request = new ConnectionOrderStatusUpdateRequest();
                request.setStatus(status.getValue());
                request.setAdminComment(comment.getValue());
                orderClient.updateStatus(token, orderId, request);
                dialog.close();
                load();
            } catch (Exception ex) {
                Notification.show("Ошибка обновления");
            }
        });
        dialog.add(new VerticalLayout(status, comment, save));
        dialog.open();
    }

    private void load() {
        try {
            String token = SessionState.getToken().orElseThrow();
            List<ConnectionOrderResponse> orders = orderClient.getAllForAdmin(
                    token,
                    statusFilter.getValue(),
                    dateFrom.getValue(),
                    dateTo.getValue()
            );
            grid.setItems(orders);
        } catch (Exception e) {
            Notification.show("Ошибка загрузки заявок");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuards.requireRole(event, Role.ADMIN);
    }
}
