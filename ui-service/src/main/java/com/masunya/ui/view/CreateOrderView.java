package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.ui.client.OrderClient;
import com.masunya.ui.client.TariffClient;
import com.masunya.ui.dto.ConnectionOrderCreateRequest;
import com.masunya.ui.dto.TariffResponse;
import com.masunya.ui.security.SessionState;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
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
    private final TextField fullName = new TextField("ФИО");
    private final TextField address = new TextField("Адрес");
    private final TextField phone = new TextField("Телефон");
    private final Button submit = new Button("Отправить");
    private List<TariffResponse> tariffs = List.of();

    public CreateOrderView(TariffClient tariffClient, OrderClient orderClient) {
        this.tariffClient = tariffClient;
        this.orderClient = orderClient;

        H2 title = new H2("Заявка на подключение");

        tariff.setItemLabelGenerator(TariffResponse::getName);
        tariff.setRequiredIndicatorVisible(true);

        fullName.setRequiredIndicatorVisible(true);
        fullName.setValueChangeMode(ValueChangeMode.EAGER);

        address.setRequiredIndicatorVisible(true);
        address.setValueChangeMode(ValueChangeMode.EAGER);

        phone.setPattern(PHONE_REGEX);
        phone.setAllowedCharPattern("[0-9+]");
        phone.setMinLength(10);
        phone.setMaxLength(16);
        phone.setErrorMessage("Введите телефон в формате +79991234567");
        phone.setPlaceholder("+79991234567");
        phone.setHelperText("Формат: +79991234567");
        phone.setRequiredIndicatorVisible(true);
        phone.setValueChangeMode(ValueChangeMode.EAGER);

        // Кнопка активируется только когда вся форма валидна.
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.setEnabled(false);
        submit.addClickListener(e -> submitForm());

        tariff.addValueChangeListener(e -> updateSubmitState());
        fullName.addValueChangeListener(e -> updateSubmitState());
        address.addValueChangeListener(e -> updateSubmitState());
        phone.addValueChangeListener(e -> updateSubmitState());

        add(title, tariff, fullName, address, phone, submit);
        setMaxWidth("500px");
        loadTariffs();
        updateSubmitState();
    }

    private void submitForm() {
        // Финальная проверка перед отправкой на backend.
        if (!isFormValid(true)) {
            return;
        }

        try {
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
            updateSubmitState();
        } catch (Exception ex) {
            Notification.show("Ошибка отправки заявки");
        }
    }

    private void updateSubmitState() {
        // Мгновенно синхронизируем состояние кнопки с валидностью формы.
        submit.setEnabled(isFormValid(false));
    }

    private boolean isFormValid(boolean markInvalid) {
        boolean tariffValid = tariff.getValue() != null;
        boolean fullNameValid = hasText(fullName.getValue());
        boolean addressValid = hasText(address.getValue());
        String rawPhone = phone.getValue() == null ? "" : phone.getValue().trim();
        // Валидация номера телефона по шаблону +79991234567.
        boolean phoneValid = rawPhone.matches(PHONE_REGEX);

        if (markInvalid || !fullName.isEmpty()) {
            fullName.setInvalid(!fullNameValid);
        }
        if (markInvalid || !address.isEmpty()) {
            address.setInvalid(!addressValid);
        }
        if (markInvalid || !rawPhone.isEmpty()) {
            phone.setInvalid(!phoneValid);
        } else if (!markInvalid) {
            phone.setInvalid(false);
        }

        return tariffValid && fullNameValid && addressValid && phoneValid;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
        updateSubmitState();
    }
}
