package com.company.itpearls.web.screens.mainscreen;

import com.company.itpearls.BeanNotificationEvent;
import com.company.itpearls.UiNotificationEvent;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.web.app.main.MainScreen;
import org.springframework.context.event.EventListener;
import com.haulmont.cuba.gui.components.ContentMode;

import javax.inject.Inject;


@UiController("extMainScreen")
@UiDescriptor("ext-main-screen.xml")
public class ExtMainScreen extends MainScreen {
    @Inject
    private Notifications notifications;

    @EventListener
    public void onUiNotificationEvent(UiNotificationEvent event) {
        if (event.getMessage().startsWith("Открыта") ||
                event.getMessage().startsWith("Закрыта")) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withDescription(event.getMessage())
                    .withCaption("INFO")
                    .withContentMode(ContentMode.HTML)
                    .show();
        } else {
            notifications.create(Notifications.NotificationType.TRAY)
                    .withDescription(event.getMessage())
                    .withCaption("INFO")
                    .withContentMode(ContentMode.HTML)
                    .show();
        }
    }

    // screens do not receive non-UI events!
    @EventListener
    public void onBeanNotificationEvent(BeanNotificationEvent event) {
        throw new IllegalStateException("Received " + event);
    }
}