package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.ui.client.AuthClient;
import com.masunya.ui.dto.AuthResponse;
import com.masunya.ui.dto.LoginRequest;
import com.masunya.ui.security.SessionState;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route(value = "login", layout = MainLayout.class)
@PageTitle("Вход")
public class LoginView extends VerticalLayout {
    public LoginView(AuthClient authClient) {
        H2 title = new H2("Вход");
        TextField username = new TextField("Логин");
        PasswordField password = new PasswordField("Пароль");
        Button submit = new Button("Войти");

        submit.addClickListener(e -> {
            try {
                LoginRequest request = new LoginRequest();
                request.setUsername(username.getValue());
                request.setPassword(password.getValue());
                AuthResponse response = authClient.login(request);
                if (response == null || response.getToken() == null) {
                    Notification.show("Не удалось войти");
                    return;
                }
                SessionState.setToken(response.getToken());
                VaadinSession.getCurrent().access(() -> {
                    if (SessionState.hasRole(Role.ADMIN)) {
                        getUI().ifPresent(ui -> ui.navigate(AdminOrdersView.class));
                    } else {
                        getUI().ifPresent(ui -> ui.navigate(TariffListView.class));
                    }
                });
            } catch (Exception ex) {
                Notification.show("Ошибка входа");
            }
        });

        setMaxWidth("400px");
        setSpacing(true);
        add(title, username, password, submit);
    }
}
