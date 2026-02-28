package com.masunya.ui.view;

import com.masunya.common.enumerate.Role;
import com.masunya.ui.security.SessionState;
import com.vaadin.flow.router.BeforeEnterEvent;

public final class ViewGuards {
    private ViewGuards() {
    }

    public static void requireAuth(BeforeEnterEvent event) {
        if (!SessionState.isAuthenticated()) {
            event.rerouteTo(LoginView.class);
        }
    }

    public static void requireRole(BeforeEnterEvent event, Role role) {
        if (!SessionState.isAuthenticated()) {
            event.rerouteTo(LoginView.class);
            return;
        }
        if (!SessionState.hasRole(role)) {
            event.rerouteTo(TariffListView.class);
        }
    }
}
