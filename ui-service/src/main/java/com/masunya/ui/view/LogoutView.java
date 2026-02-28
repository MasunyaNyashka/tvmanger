package com.masunya.ui.view;

import com.masunya.ui.security.SessionState;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "logout", layout = MainLayout.class)
@PageTitle("Выход")
public class LogoutView extends VerticalLayout {
    public LogoutView() {
        SessionState.clear();
        UI.getCurrent().navigate(TariffListView.class);
    }
}
