package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.ui.client.OrderClient;
import com.masunya.ui.dto.ClientTariffResponse;
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

@Route(value = "tariffs/my", layout = MainLayout.class)
@PageTitle("Мои тарифы")
public class MyTariffsView extends VerticalLayout implements BeforeEnterObserver {
    private final OrderClient orderClient;
    private final Grid<ClientTariffResponse> grid = new Grid<>(ClientTariffResponse.class, false);

    public MyTariffsView(OrderClient orderClient) {
        this.orderClient = orderClient;

        H2 title = new H2("Мои подключенные тарифы");
        Button refresh = new Button("Обновить", e -> load());

        grid.addColumn(ClientTariffResponse::getId).setHeader("ID записи").setAutoWidth(true);
        grid.addColumn(ClientTariffResponse::getTariffId).setHeader("Tariff ID").setAutoWidth(true);
        grid.addColumn(ClientTariffResponse::getCustomPrice).setHeader("Персональная цена").setAutoWidth(true);
        grid.addColumn(ClientTariffResponse::getCustomConditions).setHeader("Персональные условия").setAutoWidth(true);
        grid.addColumn(ClientTariffResponse::getCreatedAt).setHeader("Создано").setAutoWidth(true);
        grid.addColumn(ClientTariffResponse::getUpdatedAt).setHeader("Обновлено").setAutoWidth(true);

        add(title, refresh, grid);
        setSizeFull();
        load();
    }

    private void load() {
        try {
            String token = SessionState.getToken().orElseThrow();
            List<ClientTariffResponse> items = orderClient.getMyClientTariffs(token);
            grid.setItems(items);
        } catch (Exception e) {
            Notification.show("Не удалось загрузить тарифы");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuards.requireRole(event, Role.CLIENT);
    }
}
