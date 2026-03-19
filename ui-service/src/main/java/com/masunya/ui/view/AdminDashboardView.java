package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.ui.client.OrderClient;
import com.masunya.ui.client.ServiceRequestClient;
import com.masunya.ui.client.TariffClient;
import com.masunya.ui.dto.ConnectionOrderResponse;
import com.masunya.ui.dto.ServiceRequestResponse;
import com.masunya.ui.dto.TariffResponse;
import com.masunya.ui.security.SessionState;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Route(value = "admin/dashboard", layout = MainLayout.class)
@PageTitle("Дашборд")
public class AdminDashboardView extends VerticalLayout implements BeforeEnterObserver {
    private final OrderClient orderClient;
    private final ServiceRequestClient serviceRequestClient;
    private final TariffClient tariffClient;

    private final DatePicker dateFrom = new DatePicker("Дата с");
    private final DatePicker dateTo = new DatePicker("Дата по");
    private final Span totals = new Span();
    private final Grid<TariffStatRow> topTariffsGrid = new Grid<>(TariffStatRow.class, false);
    private final Grid<HourStatRow> ordersByHourGrid = new Grid<>(HourStatRow.class, false);
    private final Grid<HourStatRow> serviceRequestsByHourGrid = new Grid<>(HourStatRow.class, false);

    public AdminDashboardView(
            OrderClient orderClient,
            ServiceRequestClient serviceRequestClient,
            TariffClient tariffClient
    ) {
        this.orderClient = orderClient;
        this.serviceRequestClient = serviceRequestClient;
        this.tariffClient = tariffClient;

        H2 title = new H2("Дашборд администратора");
        dateFrom.setValue(LocalDate.now());
        dateTo.setValue(LocalDate.now());

        Button refresh = new Button("Обновить", e -> load());
        refresh.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout filters = new HorizontalLayout(dateFrom, dateTo, refresh);
        filters.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        filters.setWidthFull();

        topTariffsGrid.addColumn(TariffStatRow::getTariffName).setHeader("Тариф").setAutoWidth(true);
        topTariffsGrid.addColumn(TariffStatRow::getCount).setHeader("Заявок").setAutoWidth(true);
        topTariffsGrid.setWidthFull();
        topTariffsGrid.setAllRowsVisible(true);

        ordersByHourGrid.addColumn(HourStatRow::getHour).setHeader("Час").setAutoWidth(true);
        ordersByHourGrid.addColumn(HourStatRow::getCount).setHeader("Заявок").setAutoWidth(true);
        ordersByHourGrid.setWidthFull();
        ordersByHourGrid.setAllRowsVisible(true);

        serviceRequestsByHourGrid.addColumn(HourStatRow::getHour).setHeader("Час").setAutoWidth(true);
        serviceRequestsByHourGrid.addColumn(HourStatRow::getCount).setHeader("Сервисных заявок").setAutoWidth(true);
        serviceRequestsByHourGrid.setWidthFull();
        serviceRequestsByHourGrid.setAllRowsVisible(true);

        add(
                title,
                filters,
                totals,
                new H2("Топ тарифов"),
                topTariffsGrid,
                new H2("Заявки на подключение по часам"),
                ordersByHourGrid,
                new H2("Сервисные заявки по часам"),
                serviceRequestsByHourGrid
        );
        setSizeFull();
        load();
    }

    private void load() {
        try {
            String token = SessionState.getToken().orElseThrow();
            // Грузим агрегаты по заявкам в выбранном диапазоне дат.
            List<ConnectionOrderResponse> orders = orderClient.getAllForAdmin(
                    token,
                    null,
                    dateFrom.getValue(),
                    dateTo.getValue()
            );
            List<ServiceRequestResponse> serviceRequests = serviceRequestClient.getAllForAdmin(
                    token,
                    null,
                    null,
                    dateFrom.getValue(),
                    dateTo.getValue()
            );
            Map<UUID, String> tariffNameById = tariffClient.getAllForAdmin(token).stream()
                    .collect(Collectors.toMap(TariffResponse::getId, TariffResponse::getName, (a, b) -> a));

            totals.setText("Заявки на подключение: " + orders.size() + ", сервисные заявки: " + serviceRequests.size());
            topTariffsGrid.setItems(buildTopTariffs(orders, tariffNameById));
            // Строим почасовую статистику для двух типов заявок.
            ordersByHourGrid.setItems(buildHours(orders.stream().map(ConnectionOrderResponse::getCreatedAt).toList()));
            serviceRequestsByHourGrid.setItems(buildHours(
                    serviceRequests.stream().map(ServiceRequestResponse::getCreatedAt).toList()
            ));
        } catch (Exception e) {
            Notification.show("Не удалось загрузить данные дашборда");
        }
    }

    private List<TariffStatRow> buildTopTariffs(List<ConnectionOrderResponse> orders, Map<UUID, String> tariffNameById) {
        return orders.stream()
                .collect(Collectors.groupingBy(ConnectionOrderResponse::getTariffId, Collectors.counting()))
                .entrySet()
                .stream()
                .map(e -> new TariffStatRow(
                        tariffNameById.getOrDefault(e.getKey(), e.getKey().toString()),
                        e.getValue()
                ))
                .sorted(Comparator.comparingLong(TariffStatRow::getCount).reversed())
                .limit(5)
                .toList();
    }

    private List<HourStatRow> buildHours(List<java.time.Instant> instants) {
        Map<Integer, Long> countsByHour = instants.stream()
                .collect(Collectors.groupingBy(
                        i -> i.atZone(ZoneId.systemDefault()).getHour(),
                        Collectors.counting()
                ));

        return IntStream.range(0, 24)
                .mapToObj(h -> new HourStatRow(String.format("%02d:00", h), countsByHour.getOrDefault(h, 0L)))
                .toList();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuards.requireRole(event, Role.ADMIN);
    }

    public static class TariffStatRow {
        private final String tariffName;
        private final long count;

        public TariffStatRow(String tariffName, long count) {
            this.tariffName = tariffName;
            this.count = count;
        }

        public String getTariffName() {
            return tariffName;
        }

        public long getCount() {
            return count;
        }
    }

    public static class HourStatRow {
        private final String hour;
        private final long count;

        public HourStatRow(String hour, long count) {
            this.hour = hour;
            this.count = count;
        }

        public String getHour() {
            return hour;
        }

        public long getCount() {
            return count;
        }
    }
}
