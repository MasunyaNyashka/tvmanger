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

@Route(value = "orders/create", layout = MainLayout.class)
@PageTitle("Заявка на подключение")
public class CreateOrderView extends VerticalLayout implements BeforeEnterObserver {
    private final TariffClient tariffClient;
    private final OrderClient orderClient;

    public CreateOrderView(TariffClient tariffClient, OrderClient orderClient) {
        this.tariffClient = tariffClient;
        this.orderClient = orderClient;

        H2 title = new H2("Заявка на подключение");
        ComboBox<TariffResponse> tariff = new ComboBox<>("Тариф");
        tariff.setItemLabelGenerator(TariffResponse::getName);

        TextField fullName = new TextField("ФИО");
        TextField address = new TextField("Адрес");
        TextField phone = new TextField("Телефон");

        Button submit = new Button("Отправить");
        submit.addClickListener(e -> {
            try {
                String token = SessionState.getToken().orElseThrow();
                ConnectionOrderCreateRequest request = new ConnectionOrderCreateRequest();
                request.setTariffId(tariff.getValue() != null ? tariff.getValue().getId() : null);
                request.setFullName(fullName.getValue());
                request.setAddress(address.getValue());
                request.setPhone(phone.getValue());
                orderClient.create(token, request);
                Notification.show("Заявка отправлена");
                fullName.clear();
                address.clear();
                phone.clear();
            } catch (Exception ex) {
                Notification.show("Ошибка отправки заявки");
            }
        });

        add(title, tariff, fullName, address, phone, submit);
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
