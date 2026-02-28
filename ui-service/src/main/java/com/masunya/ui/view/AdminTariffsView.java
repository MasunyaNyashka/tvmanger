package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.ui.client.TariffClient;
import com.masunya.ui.dto.TariffResponse;
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

@Route(value = "admin/tariffs", layout = MainLayout.class)
@PageTitle("Тарифы (админ)")
public class AdminTariffsView extends VerticalLayout implements BeforeEnterObserver {
    private final TariffClient tariffClient;
    private final Grid<TariffResponse> grid = new Grid<>(TariffResponse.class, false);

    public AdminTariffsView(TariffClient tariffClient) {
        this.tariffClient = tariffClient;

        H2 title = new H2("Тарифы (админ)");
        Button refresh = new Button("Обновить", e -> load());

        grid.addColumn(TariffResponse::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(TariffResponse::getName).setHeader("Название").setAutoWidth(true);
        grid.addColumn(t -> t.getPrice() != null ? t.getPrice().toString() : "")
                .setHeader("Цена").setAutoWidth(true);
        grid.addColumn(t -> t.getConnectionType() != null ? t.getConnectionType().name() : "")
                .setHeader("Тип подключения").setAutoWidth(true);
        grid.addColumn(TariffResponse::isArchived).setHeader("Архив").setAutoWidth(true);

        add(title, refresh, grid);
        setSizeFull();
        load();
    }

    private void load() {
        try {
            String token = SessionState.getToken().orElseThrow();
            List<TariffResponse> tariffs = tariffClient.getAllForAdmin(token);
            grid.setItems(tariffs);
        } catch (Exception e) {
            Notification.show("Ошибка загрузки тарифов");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuards.requireRole(event, Role.ADMIN);
    }
}
