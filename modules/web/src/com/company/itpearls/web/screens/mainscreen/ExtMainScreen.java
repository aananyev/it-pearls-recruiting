package com.company.itpearls.web.screens.mainscreen;

import com.company.itpearls.BeanNotificationEvent;
import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.PersonelReserve;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.screen.MessageBundle;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.app.main.MainScreen;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import org.springframework.context.event.EventListener;
import com.haulmont.cuba.gui.components.ContentMode;

import javax.inject.Inject;
import javax.management.Notification;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;


@UiController("extMainScreen")
@UiDescriptor("ext-main-screen.xml")
public class ExtMainScreen extends MainScreen {
    @Inject
    private Notifications notifications;
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSession userSession;
    @Inject
    private Events events;

    static final String EVENT_NOTIFICATION_REMINDER = "НАПОМИНАНИЕ";
    static final String EVENT_NOTIFICATIOM_OPEN_POSITION = "Открыт";
    static final String EVENT_NOTIFICATION_CLOSE_POSITION = "Закрыт";
    static final String QUERY_GET_ITERACTIONS_FOR_NOTIFICATIONS =
            "select e from itpearls_IteractionList e " +
                    "where e.addDate between :startDate and :endDate " +
                    "and e.recrutier = :recrutier " +
                    "and e.numberIteraction >= " +
                    "(select max(g.numberIteraction) " +
                    "   from itpearls_IteractionList g " +
                    "   where g.candidate = e.candidate) " +
                    "and e.iteractionType in " +
                    "(select f from itpearls_Iteraction f where f.notificationNeedSend = true)";
    static final String QUERY_GET_EXPIRED_PERSONAL_RESERVE =
            "select e from itpearls_PersonelReserve e " +
                    "where e.date < :date " +
                    "and e.recruter = :recruter " +
                    "and e.endDate > :date";
    static final String QUERY_LIST_RECRUTERS = "select e from sec$User e where e.active = true";

    @Inject
    private MessageBundle messageBundle;

    @EventListener
    public void onUiNotificationEvent(UiNotificationEvent event) {
        List<User> users = dataManager.load(User.class)
                .query(QUERY_LIST_RECRUTERS)
                .list();
        Boolean searchUser = false;

        for (User user : users) {
            if (event.getMessage().startsWith(user.getName() + ":")) {
                if (event.getMessage().startsWith(userSession.getUser().getName())) {
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withDescription(event.getMessage())
                            .withCaption(messageBundle.getMessage("msgReceivedComment"))
                            .withType(Notifications.NotificationType.WARNING)
                            .show();

                    searchUser = true;
                }
            }
        }

        if (!searchUser) {
            if (!event.getMessage().startsWith(EVENT_NOTIFICATION_CLOSE_POSITION)) {
                if (event.getMessage().startsWith(EVENT_NOTIFICATIOM_OPEN_POSITION)) {
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
                if (event.getMessage().startsWith(EVENT_NOTIFICATION_REMINDER)) {
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption(event.getMessage())
                            .withPosition(Notifications.Position.BOTTOM_RIGHT)
                            .withDescription("")
                            .withContentMode(ContentMode.HTML)
                            .withStyleName("notification-for-me")
                            .withHideDelayMs(-1)
                            .show();
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
        }
    }

    @Subscribe
    public void onAfterShow1(AfterShowEvent event) {
        publishMyNotification();
        checkPersonalReserveCandidates();
    }

    private void checkPersonalReserveCandidates() {
        List<PersonelReserve> personelReserves = dataManager.load(PersonelReserve.class)
                .query(QUERY_GET_EXPIRED_PERSONAL_RESERVE)
                .parameter("recruter", userSession.getUser())
                .parameter("date", new Date())
                .view("personelReserve-view")
                .list();
        Date currentDate = new Date();

        for (PersonelReserve pr : personelReserves) {
            int duffDate = ((int) (pr.getEndDate().getTime() - currentDate.getTime()))
                    / (24 * 60 * 60 * 1000) + 1;

            if (duffDate <= 7 && duffDate >= 0) {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withType(Notifications.NotificationType.TRAY)
                        .withCaption("WARNING")
                        .withDescription("Через " + duffDate + " дней кончится резерв на кандидата " +
                                pr.getJobCandidate().getFullName() + ".")
                        .withHideDelayMs(5000)
                        .withPosition(Notifications.Position.BOTTOM_RIGHT)
                        .withStyleName("personal-reserve-notification-close")
                        .show();
            }
        }
    }

    private void publishMyNotification() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

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
//                .parameter("notificationType", 2)
                .list();

        for (IteractionList list : iteractionList) {
            if (list.getIteractionType() != null) {
                if (list.getIteractionType().getNotificationNeedSend() != null) {
                    if (list.getIteractionType().getNotificationNeedSend()) {
                        if (list.getIteractionType().getNotificationWhenSend() != null) {
                            String caption = EVENT_NOTIFICATION_REMINDER;
                            String desription = "<font size=2><b>" + list.getCandidate().getFullName()
                                    + " статус \""
                                    + list.getIteractionType().getIterationName()
                                    + "\" дата "
                                    + simpleDateFormat.format(list.getAddDate())
                                    + "</b></font>";
                            switch (list.getIteractionType().getNotificationWhenSend()) {
                                case 1: // не отсылать сообщение
                                    break;
                                case 2: // только создателю итерации
                                    Notifications.NotificationBuilder notification = notifications.create(Notifications.NotificationType.WARNING)
                                            .withCaption(caption)
                                            .withType(Notifications.NotificationType.HUMANIZED)
                                            .withPosition(Notifications.Position.BOTTOM_RIGHT)
                                            .withDescription(desription)
                                            .withContentMode(ContentMode.HTML)
                                            .withStyleName("notification-for-me")
                                            .withHideDelayMs(-1);

                                    if (checkNotificationNeeds(list)) {
                                        notification.show();
                                    }
                                    break;
                                case 3: // подписчику вакансии
                                    break;
                                case 4: // подписчику кандидата
                                    break;
                                case 5: // списку
                                    break;
                                case 6: // всем
                                    if (checkNotificationNeeds(list)) {
                                        events.publish(new UiNotificationEvent(this, caption + "<br>" + desription));
                                    }

                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean checkNotificationNeeds(IteractionList list) {
        GregorianCalendar startDateCal = (GregorianCalendar) Calendar.getInstance();
        startDateCal.setTimeZone(TimeZone.getDefault());

        startDateCal.setTime(list.getAddDate());
        startDateCal.set(Calendar.HOUR_OF_DAY, 0);
        startDateCal.clear(Calendar.MINUTE);
        startDateCal.clear(Calendar.SECOND);
        startDateCal.clear(Calendar.MILLISECOND);

        GregorianCalendar endDateCal = (GregorianCalendar) Calendar.getInstance();
        endDateCal.setTimeZone(TimeZone.getDefault());

        endDateCal.setTime(list.getAddDate());
        endDateCal.set(Calendar.HOUR_OF_DAY, 0);
        endDateCal.clear(Calendar.MINUTE);
        endDateCal.clear(Calendar.SECOND);
        endDateCal.clear(Calendar.MILLISECOND);

        GregorianCalendar currentDate = (GregorianCalendar) Calendar.getInstance();
        currentDate.setTimeZone(TimeZone.getDefault());

        currentDate.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
        currentDate.clear(Calendar.MINUTE);
        currentDate.clear(Calendar.SECOND);
        currentDate.clear(Calendar.MILLISECOND);

        switch (list.getIteractionType().getNotificationPeriodType()) {
            case 0: // Только текущий день
                endDateCal.add(Calendar.DAY_OF_MONTH, 1);

                break;
            case 1: // Текущая неделя с первого дня недели по по последний
                while (startDateCal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    startDateCal.add(Calendar.DATE, -1);
                }

                endDateCal.setTime(startDateCal.getTime());
                endDateCal.add(Calendar.DATE, 7);

                break;
            case 2: // Текущий неделя с даты итерации до конца недели"
                while (endDateCal.get(Calendar.DATE) != Calendar.MONDAY) {
                    endDateCal.add(Calendar.DATE, 1);
                }

                endDateCal.add(Calendar.DATE, -1);

                break;
            case 3: // Текущий месяц с первого по последнее число месяца"
                startDateCal.set(Calendar.DAY_OF_MONTH, 1);
                endDateCal.setTime(startDateCal.getTime());
                endDateCal.add(Calendar.MONTH, 1);

                break;
            case 4: // Текущий месяц с даты итерации до конца месяца"
                endDateCal.set(Calendar.DAY_OF_MONTH, 1);
                endDateCal.add(Calendar.MONTH, 1);

                break;
            case 5: // Фиксированное число дней до и после"
                if (list.getIteractionType().getNotificationBeforeAfterDay() != null) {
                    startDateCal.add(Calendar.DATE, list.getIteractionType().getNotificationBeforeAfterDay() * -1);
                    endDateCal.add(Calendar.DATE, list.getIteractionType().getNotificationBeforeAfterDay());
                } else {
                    startDateCal.add(Calendar.DATE, -5);
                    endDateCal.add(Calendar.DATE, 5);
                }

                break;
            default:
                break;
        }


        if ((startDateCal.before(currentDate)
                && endDateCal.after(currentDate))) {
            return true;
        } else {
            return false;
        }
    }

    // screens do not receive non-UI events!
    @EventListener
    public void onBeanNotificationEvent(BeanNotificationEvent event) {
        throw new IllegalStateException("Received " + event);
    }
}