package com.masunya.ui.view;

import com.masunya.ui.client.TariffClient;
import com.masunya.ui.dto.TariffResponse;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Тарифы")
public class TariffListView extends VerticalLayout {
    private final TariffClient tariffClient;
    private final Grid<TariffResponse> grid = new Grid<>(TariffResponse.class, false);

    public TariffListView(TariffClient tariffClient) {
        this.tariffClient = tariffClient;

        H2 title = new H2("Тарифы");
        Button refresh = new Button("Обновить", e -> load());

        grid.addColumn(TariffResponse::getName).setHeader("Название").setAutoWidth(true);
        grid.addColumn(t -> t.getPrice() != null ? t.getPrice().toString() : "")
                .setHeader("Цена").setAutoWidth(true);
        grid.addColumn(t -> t.getConnectionType() != null ? t.getConnectionType().name() : "")
                .setHeader("Тип подключения").setAutoWidth(true);
        grid.addColumn(t -> t.getChannels() != null ? t.getChannels().size() : 0)
                .setHeader("Каналы").setAutoWidth(true);
        grid.addComponentColumn(t -> new Button("Подключить", e ->
                UI.getCurrent().navigate("orders/create?tariffId=" + t.getId())
        )).setHeader("Действие");

        add(title, refresh, grid);
        setSizeFull();
        load();
    }

    private void load() {
        try {
            List<TariffResponse> tariffs = tariffClient.getPublicTariffs();
            grid.setItems(tariffs);
        } catch (Exception e) {
            Notification.show("Ошибка загрузки тарифов");
        }
    }
}
