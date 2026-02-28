package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.ui.client.ServiceRequestClient;
import com.masunya.ui.dto.ServiceRequestResponse;
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

@Route(value = "service-requests/my", layout = MainLayout.class)
@PageTitle("Мои сервисные заявки")
public class MyServiceRequestsView extends VerticalLayout implements BeforeEnterObserver {
    private final ServiceRequestClient serviceRequestClient;
    private final Grid<ServiceRequestResponse> grid = new Grid<>(ServiceRequestResponse.class, false);

    public MyServiceRequestsView(ServiceRequestClient serviceRequestClient) {
        this.serviceRequestClient = serviceRequestClient;

        H2 title = new H2("Мои сервисные заявки");
        Button refresh = new Button("Обновить", e -> load());

        grid.addColumn(ServiceRequestResponse::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(ServiceRequestResponse::getType).setHeader("Тип").setAutoWidth(true);
        grid.addColumn(ServiceRequestResponse::getStatus).setHeader("Статус").setAutoWidth(true);
        grid.addColumn(ServiceRequestResponse::getAdminComment).setHeader("Комментарий").setAutoWidth(true);
        grid.addColumn(ServiceRequestResponse::getCreatedAt).setHeader("Создано").setAutoWidth(true);

        add(title, refresh, grid);
        setSizeFull();
        load();
    }

    private void load() {
        try {
            String token = SessionState.getToken().orElseThrow();
            List<ServiceRequestResponse> requests = serviceRequestClient.getMyRequests(token);
            grid.setItems(requests);
        } catch (Exception e) {
            Notification.show("Ошибка загрузки заявок");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuards.requireRole(event, Role.CLIENT);
    }
}
