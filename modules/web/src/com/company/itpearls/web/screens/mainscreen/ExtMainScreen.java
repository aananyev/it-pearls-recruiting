package com.company.itpearls.web.screens.mainscreen;

import com.company.itpearls.BeanNotificationEvent;
import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.app.main.MainScreen;
import org.springframework.context.event.EventListener;
import com.haulmont.cuba.gui.components.ContentMode;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


@UiController("extMainScreen")
@UiDescriptor("ext-main-screen.xml")
public class ExtMainScreen extends MainScreen {
    @Inject
    private Notifications notifications;
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSession userSession;

    @EventListener
    public void onUiNotificationEvent(UiNotificationEvent event) {
        if (!event.getMessage().startsWith("Закрыт")) {
            if (event.getMessage().startsWith("Открыт")) {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withDescription(event.getMessage())
                        .withHideDelayMs(10000)
                        .withPosition(Notifications.Position.TOP_RIGHT)
                        .withCaption("INFO")
                        .withStyleName("open-position-notification-open")
                        .withContentMode(ContentMode.HTML)
                        .show();
            } else {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withDescription(event.getMessage())
                        .withHideDelayMs(5000)
                        .withPosition(Notifications.Position.TOP_RIGHT)
                        .withCaption("INFO")
                        .withContentMode(ContentMode.HTML)
                        .show();
            }
        } else {
            notifications.create(Notifications.NotificationType.TRAY)
                    .withDescription(event.getMessage())
                    .withCaption("WARNING")
                    .withPosition(Notifications.Position.TOP_RIGHT)
                    .withHideDelayMs(10000)
                    .withContentMode(ContentMode.HTML)
                    .withStyleName("open-position-notification-close")
                    .show();
        }
    }

    @Subscribe
    public void onAfterShow1(AfterShowEvent event) {
        publishMyNotification();
    }

    private void publishMyNotification() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        String QUERY_GET_ITERACTIONS_FOR_NOTIFICATIONS = "select e from itpearls_IteractionList e " +
                "where e.addDate between :startDate and :endDate " +
                "and e.recrutier = :recrutier " +
                "and e.iteractionType in " +
                "(select f from itpearls_Iteraction f  where f.notificationType = :notificationType)";

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        Date startDate = cal.getTime();

        cal.add(Calendar.MONTH, 1);
        Date endDate = cal.getTime();

        List<IteractionList> iteractionList = dataManager.load(IteractionList.class)
                .query(QUERY_GET_ITERACTIONS_FOR_NOTIFICATIONS)
                .view("iteractionList-view")
                .parameter("startDate", startDate)
                .parameter("endDate", endDate)
                .parameter("recrutier", userSession.getUser())
                .parameter("notificationType", 2)
                .list();

        for (IteractionList list : iteractionList) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("ВНИМАНИЕ: в этом месяце требуется действие с кандидатом")
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withDescription(list.getCandidate().getFullName()
                            + " статус "
                            + list.getIteractionType().getIterationName()
                            + " до "
                            + simpleDateFormat.format(list.getAddDate()))
                    .withContentMode(ContentMode.HTML)
                    .withStyleName("notification-for-me")
                    .show();

        }
    }

    // screens do not receive non-UI events!
    @EventListener
    public void onBeanNotificationEvent(BeanNotificationEvent event) {
        throw new IllegalStateException("Received " + event);
    }
}