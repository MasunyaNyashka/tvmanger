package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.ui.security.SessionState;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLink;

import java.util.ArrayList;
import java.util.List;

public class MainLayout extends AppLayout implements AfterNavigationObserver {
    private final Tabs tabs = new Tabs();

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 title = new H1("TV-Manager");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");

        Span userInfo = new Span(
                SessionState.getUsername().map(u -> "@" + u).orElse("guest")
        );
        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, userInfo);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.expand(title);
        addToNavbar(header);
    }

    private void createDrawer() {
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        addToDrawer(tabs);
        refreshDrawer();
    }

    private Tab createTab(String label, Class<? extends com.vaadin.flow.component.Component> view) {
        RouterLink link = new RouterLink(label, view);
        link.setHighlightCondition((l, e) -> false);
        return new Tab(link);
    }

    private Tab logoutTab() {
        RouterLink link = new RouterLink("Выход", LogoutView.class);
        link.setHighlightCondition((l, e) -> false);
        return new Tab(link);
    }

    private void refreshDrawer() {
        tabs.removeAll();
        List<Tab> items = new ArrayList<>();
        items.add(createTab("Тарифы", TariffListView.class));

        if (!SessionState.isAuthenticated()) {
            items.add(createTab("Вход", LoginView.class));
            items.add(createTab("Регистрация", RegisterView.class));
        } else if (SessionState.hasRole(Role.CLIENT)) {
            items.add(createTab("Заявка на подключение", CreateOrderView.class));
            items.add(createTab("Мои заявки", MyOrdersView.class));
            items.add(createTab("Заявка на сервис", CreateServiceRequestView.class));
            items.add(createTab("Мои сервисные заявки", MyServiceRequestsView.class));
            items.add(logoutTab());
        } else if (SessionState.hasRole(Role.ADMIN)) {
            items.add(createTab("Дашборд (админ)", AdminDashboardView.class));
            items.add(createTab("Заявки (админ)", AdminOrdersView.class));
            items.add(createTab("Сервисные заявки (админ)", AdminServiceRequestsView.class));
            items.add(createTab("Тарифы (админ)", AdminTariffsView.class));
            items.add(logoutTab());
        }

        items.forEach(tabs::add);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        refreshDrawer();
    }
}
