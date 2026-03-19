package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.common.enumerate.ServiceRequestStatus;
import com.masunya.common.enumerate.ServiceRequestType;
import com.masunya.ui.client.ServiceRequestClient;
import com.masunya.ui.dto.ServiceRequestResponse;
import com.masunya.ui.dto.ServiceRequestStatusUpdateRequest;
import com.masunya.ui.security.SessionState;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Route(value = "admin/service-requests", layout = MainLayout.class)
@PageTitle("Сервисные заявки (админ)")
public class AdminServiceRequestsView extends VerticalLayout implements BeforeEnterObserver {
    private final ServiceRequestClient serviceRequestClient;
    private final Grid<ServiceRequestResponse> grid = new Grid<>(ServiceRequestResponse.class, false);
    private final ComboBox<ServiceRequestStatus> statusFilter = new ComboBox<>("Статус");
    private final ComboBox<ServiceRequestType> typeFilter = new ComboBox<>("Тип");
    private final DatePicker dateFrom = new DatePicker("Дата с");
    private final DatePicker dateTo = new DatePicker("Дата по");

    public AdminServiceRequestsView(ServiceRequestClient serviceRequestClient) {
        this.serviceRequestClient = serviceRequestClient;

        H2 title = new H2("Сервисные заявки (админ)");
        statusFilter.setItems(ServiceRequestStatus.values());
        typeFilter.setItems(ServiceRequestType.values());

        Button refresh = new Button("Обновить", e -> load());
        refresh.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout filters = new HorizontalLayout(statusFilter, typeFilter, dateFrom, dateTo, refresh);
        filters.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        grid.addColumn(ServiceRequestResponse::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(ServiceRequestResponse::getUserId).setHeader("User ID").setAutoWidth(true);
        grid.addColumn(ServiceRequestResponse::getType).setHeader("Тип").setAutoWidth(true);
        grid.addColumn(ServiceRequestResponse::getStatus).setHeader("Статус").setAutoWidth(true);
        grid.addColumn(ServiceRequestResponse::getAdminComment).setHeader("Комментарий").setAutoWidth(true);
        grid.addColumn(ServiceRequestResponse::getCreatedAt).setHeader("Создано").setAutoWidth(true);

        Grid.Column<ServiceRequestResponse> statusActionsColumn = grid.addComponentColumn(req -> {
            Button changeStatus = new Button("Сменить статус", e -> openStatusDialog(req));
            changeStatus.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            return changeStatus;
        }).setHeader("Статус");
        statusActionsColumn.setAutoWidth(true);
        statusActionsColumn.setFlexGrow(0);
        statusActionsColumn.setWidth("180px");

        Grid.Column<ServiceRequestResponse> tariffActionsColumn = grid.addComponentColumn(req -> {
            Button openTariffs = new Button("Тарифы клиента", e ->
                    UI.getCurrent().navigate("admin/client-tariffs?userId=" + req.getUserId())
            );
            openTariffs.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            return openTariffs;
        }).setHeader("Условия тарифа");
        tariffActionsColumn.setAutoWidth(true);
        tariffActionsColumn.setFlexGrow(0);
        tariffActionsColumn.setWidth("170px");

        add(title, filters, grid);
        setSizeFull();
        setFlexGrow(1, grid);
        grid.setSizeFull();
        load();
    }

    private void openStatusDialog(ServiceRequestResponse request) {
        Dialog dialog = new Dialog();
        ComboBox<ServiceRequestStatus> status = new ComboBox<>("Новый статус");
        status.setItems(ServiceRequestStatus.values());
        status.setValue(request.getStatus());
        TextArea comment = new TextArea("Комментарий");
        comment.setValue(request.getAdminComment() == null ? "" : request.getAdminComment());

        Button save = new Button("Сохранить", e -> {
            try {
                // Не отправляем запрос, пока админ не выбрал статус.
                if (status.getValue() == null) {
                    Notification.show("Выберите статус");
                    return;
                }

                String token = SessionState.getToken().orElseThrow();
                ServiceRequestStatusUpdateRequest body = new ServiceRequestStatusUpdateRequest();
                body.setStatus(status.getValue());
                body.setAdminComment(comment.getValue());
                serviceRequestClient.updateStatus(token, request.getId(), body);

                dialog.close();
                load();
            } catch (RestClientResponseException ex) {
                Notification.show("Ошибка обновления: " + ex.getStatusCode().value());
            } catch (Exception ex) {
                Notification.show("Ошибка обновления");
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(new VerticalLayout(status, comment, save));
        dialog.open();
    }

    private void load() {
        try {
            String token = SessionState.getToken().orElseThrow();
            List<ServiceRequestResponse> requests = serviceRequestClient.getAllForAdmin(
                    token,
                    statusFilter.getValue(),
                    typeFilter.getValue(),
                    dateFrom.getValue(),
                    dateTo.getValue()
            );
            grid.setItems(requests);
        } catch (Exception e) {
            Notification.show("Ошибка загрузки заявок");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuards.requireRole(event, Role.ADMIN);
    }
}
