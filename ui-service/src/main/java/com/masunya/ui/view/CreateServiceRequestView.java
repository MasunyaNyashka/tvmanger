package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.common.enumerate.ServiceRequestType;
import com.masunya.ui.client.ServiceRequestClient;
import com.masunya.ui.client.TariffClient;
import com.masunya.ui.dto.ServiceRequestCreateRequest;
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

@Route(value = "service-requests/create", layout = MainLayout.class)
@PageTitle("Заявка на сервис")
public class CreateServiceRequestView extends VerticalLayout implements BeforeEnterObserver {
    private static final String PHONE_REGEX = "^\\+?\\d{10,15}$";

    private final TariffClient tariffClient;
    private final ServiceRequestClient serviceRequestClient;
    private final ComboBox<ServiceRequestType> type = new ComboBox<>("Тип");
    private final ComboBox<TariffResponse> tariff = new ComboBox<>("Тариф (для изменения)");
    private final TextField address = new TextField("Адрес");
    private final TextField phone = new TextField("Телефон");
    private final TextField details = new TextField("Описание");
    private final Button submit = new Button("Отправить");

    public CreateServiceRequestView(TariffClient tariffClient, ServiceRequestClient serviceRequestClient) {
        this.tariffClient = tariffClient;
        this.serviceRequestClient = serviceRequestClient;

        H2 title = new H2("Заявка на сервис");
        type.setItems(ServiceRequestType.values());
        type.setRequiredIndicatorVisible(true);

        tariff.setItemLabelGenerator(TariffResponse::getName);

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

        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.setEnabled(false);
        submit.addClickListener(e -> submitForm());

        type.addValueChangeListener(e -> updateSubmitState());
        address.addValueChangeListener(e -> updateSubmitState());
        phone.addValueChangeListener(e -> updateSubmitState());

        add(title, type, tariff, address, phone, details, submit);
        setMaxWidth("500px");
        loadTariffs();
        updateSubmitState();
    }

    private void submitForm() {
        if (!isFormValid(true)) {
            return;
        }

        try {
            String token = SessionState.getToken().orElseThrow();
            ServiceRequestCreateRequest request = new ServiceRequestCreateRequest();
            request.setType(type.getValue());
            request.setTariffId(tariff.getValue() != null ? tariff.getValue().getId() : null);
            request.setAddress(address.getValue().trim());
            request.setPhone(phone.getValue().trim());
            request.setDetails(details.getValue());
            serviceRequestClient.create(token, request);

            Notification.show("Заявка отправлена");
            type.clear();
            tariff.clear();
            address.clear();
            phone.clear();
            details.clear();
            updateSubmitState();
        } catch (Exception ex) {
            Notification.show("Ошибка отправки заявки");
        }
    }

    private void updateSubmitState() {
        submit.setEnabled(isFormValid(false));
    }

    private boolean isFormValid(boolean markInvalid) {
        boolean typeValid = type.getValue() != null;
        boolean addressValid = hasText(address.getValue());
        String rawPhone = phone.getValue() == null ? "" : phone.getValue().trim();
        boolean phoneValid = rawPhone.matches(PHONE_REGEX);

        if (markInvalid || !address.isEmpty()) {
            address.setInvalid(!addressValid);
        }
        if (markInvalid || !rawPhone.isEmpty()) {
            phone.setInvalid(!phoneValid);
        } else if (!markInvalid) {
            phone.setInvalid(false);
        }

        return typeValid && addressValid && phoneValid;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void loadTariffs() {
        try {
            List<TariffResponse> tariffs = tariffClient.getPublicTariffs();
            tariff.setItems(tariffs);
        } catch (Exception e) {
            Notification.show("Не удалось загрузить тарифы");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuards.requireRole(event, Role.CLIENT);
    }
}
