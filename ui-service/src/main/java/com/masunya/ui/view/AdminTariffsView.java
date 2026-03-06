package com.masunya.ui.view;

import com.masunya.common.enumerate.ConnectionType;
import com.masunya.common.enumerate.Role;
import com.masunya.ui.client.TariffClient;
import com.masunya.ui.dto.TariffCreateRequest;
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
import java.util.stream.Stream;

@Route(value = "admin/tariffs", layout = MainLayout.class)
@PageTitle("Тарифы (админ)")
public class AdminTariffsView extends VerticalLayout implements BeforeEnterObserver {
    private final TariffClient tariffClient;
    private final Grid<TariffResponse> grid = new Grid<>(TariffResponse.class, false);

    public AdminTariffsView(TariffClient tariffClient) {
        this.tariffClient = tariffClient;

        H2 title = new H2("Тарифы (админ)");
        Button refresh = new Button("Обновить", e -> load());
        Button create = new Button("Создать тариф", e -> openCreateDialog());
        HorizontalLayout controls = new HorizontalLayout(refresh, create);

        grid.addColumn(TariffResponse::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(TariffResponse::getName).setHeader("Название").setAutoWidth(true);
        grid.addColumn(t -> t.getPrice() != null ? t.getPrice().toString() : "")
                .setHeader("Цена").setAutoWidth(true);
        grid.addColumn(t -> t.getConnectionType() != null ? t.getConnectionType().name() : "")
                .setHeader("Тип подключения").setAutoWidth(true);
        grid.addColumn(TariffResponse::isArchived).setHeader("Архив").setAutoWidth(true);

        add(title, controls, grid);
        setSizeFull();
        load();
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog();
        TextField name = new TextField("Название");
        BigDecimalField price = new BigDecimalField("Цена");
        ComboBox<ConnectionType> connectionType = new ComboBox<>("Тип подключения");
        connectionType.setItems(ConnectionType.values());
        TextArea description = new TextArea("Описание");
        TextArea channels = new TextArea("Каналы (через запятую)");

        Button save = new Button("Сохранить", e -> {
            try {
                if (name.getValue() == null || name.getValue().isBlank()) {
                    Notification.show("Введите название");
                    return;
                }
                if (price.getValue() == null || price.getValue().doubleValue() <= 0) {
                    Notification.show("Введите корректную цену");
                    return;
                }
                if (connectionType.getValue() == null) {
                    Notification.show("Выберите тип подключения");
                    return;
                }
                List<String> channelList = channels.getValue() == null
                        ? List.of()
                        : Stream.of(channels.getValue().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();
                if (channelList.isEmpty()) {
                    Notification.show("Укажите хотя бы один канал");
                    return;
                }

                String token = SessionState.getToken().orElseThrow();
                TariffCreateRequest request = new TariffCreateRequest();
                request.setName(name.getValue().trim());
                request.setPrice(price.getValue());
                request.setConnectionType(connectionType.getValue());
                request.setDescription(description.getValue());
                request.setChannels(channelList);
                tariffClient.create(token, request);

                dialog.close();
                load();
                Notification.show("Тариф создан");
            } catch (Exception ex) {
                Notification.show("Не удалось создать тариф");
            }
        });

        dialog.add(new VerticalLayout(name, price, connectionType, description, channels, save));
        dialog.open();
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
