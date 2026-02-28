package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.ui.client.OrderClient;
import com.masunya.ui.dto.ConnectionOrderResponse;
import com.masunya.ui.security.SessionState;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "orders/my", layout = MainLayout.class)
@PageTitle("Мои заявки")
public class MyOrdersView extends VerticalLayout implements BeforeEnterObserver {
    private final OrderClient orderClient;
    private final Grid<ConnectionOrderResponse> grid = new Grid<>(ConnectionOrderResponse.class, false);

    public MyOrdersView(OrderClient orderClient) {
        this.orderClient = orderClient;

        H2 title = new H2("Мои заявки");
        Button refresh = new Button("Обновить", e -> load());

        grid.addColumn(ConnectionOrderResponse::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(ConnectionOrderResponse::getTariffId).setHeader("Tariff ID").setAutoWidth(true);
        grid.addColumn(ConnectionOrderResponse::getStatus).setHeader("Статус").setAutoWidth(true);
        grid.addColumn(ConnectionOrderResponse::getAdminComment).setHeader("Комментарий").setAutoWidth(true);
        grid.addColumn(ConnectionOrderResponse::getCreatedAt).setHeader("Создано").setAutoWidth(true);

        add(title, refresh, grid);
        setSizeFull();
        load();
    }

    private void load() {
        try {
            String token = SessionState.getToken().orElseThrow();
            List<ConnectionOrderResponse> orders = orderClient.getMyOrders(token);
            grid.setItems(orders);
        } catch (Exception e) {
            Notification.show("Ошибка загрузки заявок");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuards.requireRole(event, Role.CLIENT);
    }
}
