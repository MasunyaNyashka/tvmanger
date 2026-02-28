package com.masunya.ui.view;

import com.masunya.ui.client.AuthClient;
import com.masunya.ui.dto.AuthResponse;
import com.masunya.ui.dto.RegisterRequest;
import com.masunya.ui.security.SessionState;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "register", layout = MainLayout.class)
@PageTitle("Регистрация")
public class RegisterView extends VerticalLayout {
    public RegisterView(AuthClient authClient) {
        H2 title = new H2("Регистрация");
        TextField username = new TextField("Логин");
        PasswordField password = new PasswordField("Пароль");
        Button submit = new Button("Зарегистрироваться");

        submit.addClickListener(e -> {
            try {
                RegisterRequest request = new RegisterRequest();
                request.setUsername(username.getValue());
                request.setPassword(password.getValue());
                AuthResponse response = authClient.register(request);
                if (response == null || response.getToken() == null) {
                    Notification.show("Не удалось зарегистрироваться");
                    return;
                }
                SessionState.setToken(response.getToken());
                getUI().ifPresent(ui -> ui.navigate(TariffListView.class));
            } catch (Exception ex) {
                Notification.show("Ошибка регистрации");
            }
        });

        setMaxWidth("400px");
        setSpacing(true);
        add(title, username, password, submit);
    }
}
