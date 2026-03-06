package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.ui.client.OrderClient;
import com.masunya.ui.client.TariffClient;
import com.masunya.ui.dto.ClientTariffResponse;
import com.masunya.ui.dto.ClientTariffUpdateRequest;
import com.masunya.ui.dto.TariffResponse;
import com.masunya.ui.security.SessionState;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Route(value = "admin/client-tariffs", layout = MainLayout.class)
@PageTitle("Тарифы клиентов (админ)")
public class AdminClientTariffsView extends VerticalLayout implements BeforeEnterObserver {
    private final OrderClient orderClient;
    private final TariffClient tariffClient;
    private final Grid<ClientTariffResponse> grid = new Grid<>(ClientTariffResponse.class, false);
    private final TextField userIdFilter = new TextField("User ID (фильтр)");
    private List<TariffResponse> availableTariffs = List.of();

    public AdminClientTariffsView(OrderClient orderClient, TariffClient tariffClient) {
        this.orderClient = orderClient;
        this.tariffClient = tariffClient;

        H2 title = new H2("Персональные тарифы клиентов");
        Button loadButton = new Button("Загрузить", e -> loadItems());
        HorizontalLayout controls = new HorizontalLayout(userIdFilter, loadButton);

        grid.addColumn(ClientTariffResponse::getId).setHeader("ID записи").setAutoWidth(true);
        grid.addColumn(ClientTariffResponse::getUserId).setHeader("User ID").setAutoWidth(true);
        grid.addColumn(ClientTariffResponse::getTariffId).setHeader("Tariff ID").setAutoWidth(true);
        grid.addColumn(ClientTariffResponse::getCustomPrice).setHeader("Персональная цена").setAutoWidth(true);
        grid.addColumn(ClientTariffResponse::getCustomConditions).setHeader("Условия").setAutoWidth(true);
        grid.addColumn(ClientTariffResponse::getUpdatedAt).setHeader("Обновлено").setAutoWidth(true);
        grid.addComponentColumn(item -> new Button("Изменить", e -> openEditDialog(item))).setHeader("Действия");

        add(title, controls, grid);
        setSizeFull();
        loadTariffs();
        loadItems();
    }

    private void loadTariffs() {
        try {
            String token = SessionState.getToken().orElseThrow();
            availableTariffs = tariffClient.getAllForAdmin(token);
        } catch (Exception e) {
            Notification.show("Не удалось загрузить тарифы");
        }
    }

    private void loadItems() {
        try {
            String token = SessionState.getToken().orElseThrow();
            UUID userId = parseOptionalUuid(userIdFilter.getValue());
            List<ClientTariffResponse> items = orderClient.getClientTariffsForAdmin(token, userId);
            grid.setItems(items);
        } catch (IllegalArgumentException e) {
            Notification.show("Некорректный User ID");
        } catch (Exception e) {
            Notification.show("Не удалось загрузить данные");
        }
    }

    private void openEditDialog(ClientTariffResponse item) {
        Dialog dialog = new Dialog();
        ComboBox<TariffResponse> tariff = new ComboBox<>("Новый базовый тариф");
        tariff.setItems(availableTariffs);
        tariff.setItemLabelGenerator(TariffResponse::getName);
        tariff.setValue(availableTariffs.stream().filter(t -> t.getId().equals(item.getTariffId())).findFirst().orElse(null));

        BigDecimalField price = new BigDecimalField("Персональная цена");
        price.setValue(item.getCustomPrice());
        TextArea conditions = new TextArea("Персональные условия");
        conditions.setValue(item.getCustomConditions() == null ? "" : item.getCustomConditions());

        Button save = new Button("Сохранить", e -> {
            try {
                if (tariff.getValue() == null) {
                    Notification.show("Выберите тариф");
                    return;
                }
                String token = SessionState.getToken().orElseThrow();
                ClientTariffUpdateRequest request = new ClientTariffUpdateRequest();
                request.setTariffId(tariff.getValue().getId());
                request.setCustomPrice(price.getValue());
                request.setCustomConditions(conditions.getValue());
                orderClient.updateClientTariff(token, item.getId(), request);
                dialog.close();
                loadItems();
            } catch (Exception ex) {
                Notification.show("Не удалось сохранить изменения");
            }
        });
        dialog.add(new VerticalLayout(tariff, price, conditions, save));
        dialog.open();
    }

    private UUID parseOptionalUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return UUID.fromString(raw.trim());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuards.requireRole(event, Role.ADMIN);
        Optional<String> userId = event.getLocation()
                .getQueryParameters()
                .getParameters()
                .getOrDefault("userId", List.of())
                .stream()
                .findFirst();
        userId.ifPresent(userIdFilter::setValue);
        if (userId.isPresent()) {
            loadItems();
        }
    }
}
