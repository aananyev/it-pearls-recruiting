package com.company.itpearls.web.screens.interviewcalendar;

import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Calendar;
import com.haulmont.cuba.gui.components.calendar.CalendarEventProvider;
import com.haulmont.cuba.gui.components.calendar.SimpleCalendarEvent;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.text.DateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@UiController("itpearls_WeekInterviewCalendar")
@UiDescriptor("week-interview-calendar.xml")
public class WeekInterviewCalendar extends Screen {
    @Inject
    private Calendar<Date> interviewCalendar;
    @Inject
    private CollectionContainer<IteractionList> calendarDataDc;
    @Inject
    private CollectionLoader<IteractionList> calendarDataDl;
    @Inject
    private CheckBox huntingCheckBox;
    @Inject
    private CheckBox toConpanyCheckBox;
    @Inject
    private DatePicker<java.sql.Date> monthPicker;
    @Inject
    private Notifications notifications;
    @Inject
    private UserSession userSession;
    @Inject
    private DataManager dataManager;

    private String nameCandidate = "Имя: ";
    private String typeIteraction = "Тип: ";
    @Inject
    private ScreenBuilders screenBuilders;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        huntingCheckBox.setValue(true);
        toConpanyCheckBox.setValue(true);

        calendarDataDl.load();

        updateCalendar();
        setCalendarEventListener();
        setDaysOfWeekNames();
    }

    String getTime(Date date) {
        GregorianCalendar calendar = new GregorianCalendar();

        calendar.setTimeZone(userSession.getTimeZone());
        calendar.setTime(date);

        return calendar.get(GregorianCalendar.HOUR) + ":" + calendar.get(GregorianCalendar.MINUTE);
    }

    private void setCalendarEventListener() {
        interviewCalendar.addEventClickListener(dateCalendarEventClickEvent -> {
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, userSession.getLocale());

            notifications.create()
                    .withCaption(df.format(dateCalendarEventClickEvent.getCalendarEvent().getStart()) + ": " +
                            getTime(dateCalendarEventClickEvent.getCalendarEvent().getStart()) + " - " +
                            getTime(dateCalendarEventClickEvent.getCalendarEvent().getEnd()))
                    .withDescription(dateCalendarEventClickEvent.getCalendarEvent().getDescription())
                    .withType(Notifications.NotificationType.TRAY)
                    .withHtmlSanitizer(false)
                    .show();

            String a = getCandidateName(dateCalendarEventClickEvent.getCalendarEvent().getCaption());

            JobCandidate jobCandidate = dataManager.load(JobCandidate.class)
                    .query("select e from itpearls_JobCandidate e where e.fullName = \'"+a+"\'")
                    .view("jobCandidate-view")
                    .one();

            IteractionList iteractionList = dataManager.load(IteractionList.class)
                    .query("select e " +
                            "from itpearls_IteractionList e " +
                            "where e.addDate = :dateIteraction and e.candidate = " +
                            "(select f from itpearls_JobCandidate f " +
                            "where f.fullName like :candidate)")
                    .parameter("dateIteraction", dateCalendarEventClickEvent.getCalendarEvent().getStart())
                    .parameter("candidate", getCandidateName(dateCalendarEventClickEvent.getCalendarEvent().getCaption()))
                    .view("iteractionList-view")
                    .one();

            if (iteractionList != null) {
                screenBuilders.editor(IteractionList.class, this)
                        .editEntity(iteractionList)
                        .withOpenMode(OpenMode.DIALOG)
                        .build()
                        .show();
            }
        });
    }

    private String getCandidateName(String caption) {
        return caption;
    }

    private void setDaysOfWeekNames() {
        // Первый день недели - понедельник
        interviewCalendar.unwrap(com.vaadin.v7.ui.Calendar.class).setFirstDayOfWeek(java.util.Calendar.MONDAY);

        Map<DayOfWeek, String> days = new HashMap<>(7);
        days.put(DayOfWeek.MONDAY, "Понедельник\n");
        days.put(DayOfWeek.TUESDAY, "Вторник\n");
        days.put(DayOfWeek.WEDNESDAY, "Среда\n");
        days.put(DayOfWeek.THURSDAY, "Четверг\n");
        days.put(DayOfWeek.FRIDAY, "Пятница\n");
        days.put(DayOfWeek.SATURDAY, "Суббота\n");
        days.put(DayOfWeek.SUNDAY, "Воскресенье\n");
        interviewCalendar.setDayNames(days);
    }

    public static Date atStartOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
        return localDateTimeToDate(startOfDay);
    }

    public static Date atEndOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
        return localDateTimeToDate(endOfDay);
    }

    private static LocalDateTime dateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private void updateCalendar() {
        CalendarEventProvider eventProvider = interviewCalendar.getEventProvider();
        eventProvider.removeAllEvents();

        for (IteractionList list : calendarDataDc.getItems()) {
            Date eventDate = list.getAddDate();

            SimpleCalendarEvent calendarEvent = new SimpleCalendarEvent();
            calendarEvent.setCaption(list.getCandidate().getFullName());
            calendarEvent.setStart(eventDate);

            // добавим час по умолчанию
            GregorianCalendar calendar = new GregorianCalendar();

            calendar.setTime(eventDate);
            calendar.add(GregorianCalendar.HOUR, 1);
            eventDate = calendar.getTime();

            calendarEvent.setEnd(eventDate);

            if (list.getIteractionType().getCalendarItemStyle() != null)
                calendarEvent.setStyleName(list.getIteractionType().getCalendarItemStyle());

            if (list.getIteractionType().getCalendarItemDescription() != null)
                calendarEvent.setDescription(typeIteraction +
                        (list.getProject() != null ? list.getProject().getProjectName() + ":" : "") +
                        list.getIteractionType().getCalendarItemDescription() +
                        "\n" + nameCandidate +
                        list.getCandidate().getFullName() + "");
            else
                calendarEvent.setDescription(typeIteraction +
                        (list.getProject() != null ? list.getProject().getProjectName() + ":\n" : "") +
                        nameCandidate +
                        list.getCandidate().getFullName() + "");

            calendarEvent.setAllDay(false);

            interviewCalendar.getEventProvider().addEvent(calendarEvent);
        }
    }

    @Subscribe("huntingCheckBox")
    public void onHuntingCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (huntingCheckBox.getValue()) {
            calendarDataDl.setParameter("numberInternal", "002" + "%");
        } else {
            if (!toConpanyCheckBox.getValue())
                calendarDataDl.setParameter("numberInternal", "nodata");
            else
                calendarDataDl.removeParameter("numberInternal");
        }

        calendarDataDl.load();

        updateCalendar();
    }

    @Subscribe("toConpanyCheckBox")
    public void onToConpanyCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (toConpanyCheckBox.getValue()) {
            calendarDataDl.setParameter("numberExternal", "003" + "%");
        } else {
            if (!huntingCheckBox.getValue())
                calendarDataDl.setParameter("numberExternal", "nodata");
            else
                calendarDataDl.removeParameter("numberExternal");
        }

        calendarDataDl.load();

        updateCalendar();
    }

    @Subscribe("monthPicker")
    public void onMonthPickerValueChange(HasValue.ValueChangeEvent<java.sql.Date> event) {
        java.util.Calendar c = new GregorianCalendar();
        c.setTime(event.getValue());
        c.add(java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
        interviewCalendar.setStartDate((event.getValue()));
        interviewCalendar.setEndDate(c.getTime());

//        reformatCalendar();
    }
}