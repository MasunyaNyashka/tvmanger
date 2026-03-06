package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.ui.client.OrderClient;
import com.masunya.ui.client.TariffClient;
import com.masunya.ui.dto.ConnectionOrderCreateRequest;
import com.masunya.ui.dto.TariffResponse;
import com.masunya.ui.security.SessionState;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Route(value = "orders/create", layout = MainLayout.class)
@PageTitle("Заявка на подключение")
public class CreateOrderView extends VerticalLayout implements BeforeEnterObserver {
    private static final String PHONE_REGEX = "^\\+?\\d{10,15}$";

    private final TariffClient tariffClient;
    private final OrderClient orderClient;
    private final ComboBox<TariffResponse> tariff = new ComboBox<>("Тариф");
    private List<TariffResponse> tariffs = List.of();

    public CreateOrderView(TariffClient tariffClient, OrderClient orderClient) {
        this.tariffClient = tariffClient;
        this.orderClient = orderClient;

        H2 title = new H2("Заявка на подключение");
        tariff.setItemLabelGenerator(TariffResponse::getName);
        tariff.setRequiredIndicatorVisible(true);

        TextField fullName = new TextField("ФИО");
        fullName.setRequiredIndicatorVisible(true);
        TextField address = new TextField("Адрес");
        address.setRequiredIndicatorVisible(true);

        TextField phone = new TextField("Телефон");
        phone.setPattern(PHONE_REGEX);
        phone.setAllowedCharPattern("[0-9+]");
        phone.setMinLength(10);
        phone.setMaxLength(16);
        phone.setErrorMessage("Введите телефон в формате +79991234567");
        phone.setPlaceholder("+79991234567");
        phone.setHelperText("Формат: +79991234567");
        phone.setRequiredIndicatorVisible(true);

        Button submit = new Button("Отправить");
        submit.addClickListener(e -> {
            try {
                if (!validateForm(fullName, address, phone)) {
                    return;
                }

                String token = SessionState.getToken().orElseThrow();
                ConnectionOrderCreateRequest request = new ConnectionOrderCreateRequest();
                request.setTariffId(tariff.getValue().getId());
                request.setFullName(fullName.getValue().trim());
                request.setAddress(address.getValue().trim());
                request.setPhone(phone.getValue().trim());
                orderClient.create(token, request);

                Notification.show("Заявка отправлена");
                fullName.clear();
                address.clear();
                phone.clear();
                tariff.clear();
            } catch (Exception ex) {
                Notification.show("Ошибка отправки заявки");
            }
        });

        add(title, tariff, fullName, address, phone, submit);
        setMaxWidth("500px");
        loadTariffs();
    }

    private boolean validateForm(TextField fullName, TextField address, TextField phone) {
        boolean valid = true;

        if (tariff.getValue() == null) {
            Notification.show("Выберите тариф");
            valid = false;
        }

        if (fullName.getValue() == null || fullName.getValue().isBlank()) {
            fullName.setInvalid(true);
            valid = false;
        } else {
            fullName.setInvalid(false);
        }

        if (address.getValue() == null || address.getValue().isBlank()) {
            address.setInvalid(true);
            valid = false;
        } else {
            address.setInvalid(false);
        }

        String rawPhone = phone.getValue() == null ? "" : phone.getValue().trim();
        if (!rawPhone.matches(PHONE_REGEX)) {
            phone.setInvalid(true);
            valid = false;
        } else {
            phone.setInvalid(false);
        }

        if (!valid) {
            Notification.show("Проверьте заполнение формы");
        }
        return valid;
    }

    private void loadTariffs() {
        try {
            tariffs = tariffClient.getPublicTariffs();
            tariff.setItems(tariffs);
        } catch (Exception e) {
            Notification.show("Не удалось загрузить тарифы");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuards.requireRole(event, Role.CLIENT);
        Optional<String> tariffId = event.getLocation()
                .getQueryParameters()
                .getParameters()
                .getOrDefault("tariffId", List.of())
                .stream()
                .findFirst();
        tariffId.ifPresent(rawId -> {
            try {
                UUID id = UUID.fromString(rawId);
                tariffs.stream()
                        .filter(t -> id.equals(t.getId()))
                        .findFirst()
                        .ifPresent(tariff::setValue);
            } catch (Exception ignored) {
            }
        });
    }
}
