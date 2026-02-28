package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.common.enumerate.ServiceRequestType;
import com.masunya.ui.client.ServiceRequestClient;
import com.masunya.ui.client.TariffClient;
import com.masunya.ui.dto.ServiceRequestCreateRequest;
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

@Route(value = "service-requests/create", layout = MainLayout.class)
@PageTitle("Заявка на сервис")
public class CreateServiceRequestView extends VerticalLayout implements BeforeEnterObserver {
    private final TariffClient tariffClient;
    private final ServiceRequestClient serviceRequestClient;

    public CreateServiceRequestView(TariffClient tariffClient, ServiceRequestClient serviceRequestClient) {
        this.tariffClient = tariffClient;
        this.serviceRequestClient = serviceRequestClient;

        H2 title = new H2("Заявка на сервис");
        ComboBox<ServiceRequestType> type = new ComboBox<>("Тип");
        type.setItems(ServiceRequestType.values());

        ComboBox<TariffResponse> tariff = new ComboBox<>("Тариф (для изменения)");
        tariff.setItemLabelGenerator(TariffResponse::getName);

        TextField address = new TextField("Адрес");
        TextField phone = new TextField("Телефон");
        TextField details = new TextField("Описание");

        Button submit = new Button("Отправить");
        submit.addClickListener(e -> {
            try {
                String token = SessionState.getToken().orElseThrow();
                ServiceRequestCreateRequest request = new ServiceRequestCreateRequest();
                request.setType(type.getValue());
                request.setTariffId(tariff.getValue() != null ? tariff.getValue().getId() : null);
                request.setAddress(address.getValue());
                request.setPhone(phone.getValue());
                request.setDetails(details.getValue());
                serviceRequestClient.create(token, request);
                Notification.show("Заявка отправлена");
                address.clear();
                phone.clear();
                details.clear();
            } catch (Exception ex) {
                Notification.show("Ошибка отправки заявки");
            }
        });

        add(title, type, tariff, address, phone, details, submit);
        setMaxWidth("500px");
        loadTariffs(tariff);
    }

    private void loadTariffs(ComboBox<TariffResponse> tariff) {
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
