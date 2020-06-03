package com.company.itpearls.web.screens.interviewcalendar;

import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Calendar;
import com.haulmont.cuba.gui.components.calendar.CalendarEventProvider;
import com.haulmont.cuba.gui.components.calendar.SimpleCalendarEvent;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
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
    private VBoxLayout vboxMain;
    @Inject
    private RadioButtonGroup typeCalendar;
    @Inject
    private DatePicker<java.sql.Date> monthPicker;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        huntingCheckBox.setValue( true );
        toConpanyCheckBox.setValue( true );


        calendarDataDl.load();

        createTypeCalendarRadioButton();

        updateCalendar();

        setDaysOfWeekNames();
    }

    private void setDaysOfWeekNames() {
        // Первый день недели - понедельник
        interviewCalendar.unwrap(com.vaadin.v7.ui.Calendar.class).setFirstDayOfWeek(java.util.Calendar.MONDAY);

        Map<DayOfWeek, String> days = new HashMap<>(7);
        days.put(DayOfWeek.MONDAY,"Понедельник");
        days.put(DayOfWeek.TUESDAY,"Вторник");
        days.put(DayOfWeek.WEDNESDAY,"Среда");
        days.put(DayOfWeek.THURSDAY,"Четверг");
        days.put(DayOfWeek.FRIDAY,"Пятница");
        days.put(DayOfWeek.SATURDAY,"Суббота");
        days.put(DayOfWeek.SUNDAY,"Воскресенье");
        interviewCalendar.setDayNames(days);
    }

    private void createTypeCalendarRadioButton() {
        Map<String, Integer> typeCalendarRadioButton = new LinkedHashMap<>();

        typeCalendarRadioButton.put( "День", 1 );
        typeCalendarRadioButton.put( "Неделя", 2 );
        typeCalendarRadioButton.put( "Месяц", 3 );

        typeCalendar.setOptionsMap( typeCalendarRadioButton );
        typeCalendar.setValue( 3 );
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

    @Subscribe("typeCalendar")
    public void onTypeCalendarValueChange(HasValue.ValueChangeEvent event) {

//        reformatCalendar();
    }

    private void reformatCalendar() {
        Date currentDate = new Date();

        if( typeCalendar.getValue().equals( 1 ) ) {
            interviewCalendar.setStartDate( atStartOfDay( currentDate ) );
            interviewCalendar.setEndDate( atEndOfDay( currentDate ));

            monthPicker.setResolution( DatePicker.Resolution.DAY );
        }

        if( typeCalendar.getValue().equals( 2 ) ) {

            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setFirstDayOfWeek(java.util.Calendar.MONDAY );
//            calendar.setTime( new Date() );

            int today = calendar.get(java.util.Calendar.DAY_OF_WEEK );
            calendar.add(java.util.Calendar.DAY_OF_WEEK, -today + java.util.Calendar.MONDAY );
            interviewCalendar.setStartDate( calendar.getTime() );

            calendar.add( GregorianCalendar.DAY_OF_MONTH, 6 );
            interviewCalendar.setEndDate( calendar.getTime() );

            monthPicker.setResolution( DatePicker.Resolution.MONTH );
        }

        if( typeCalendar.getValue().equals( 3 ) ) {
            java.util.Calendar c = new GregorianCalendar();
            c.set( java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(c.DAY_OF_MONTH));
            interviewCalendar.setEndDate( c.getTime() );

            c.set( java.util.Calendar.DAY_OF_MONTH, c.getActualMinimum(c.DAY_OF_MONTH));
            interviewCalendar.setStartDate( c.getTime() );

            monthPicker.setResolution( DatePicker.Resolution.MONTH);
        }

        // updateCalendar();
    }

    private void updateCalendar() {
        CalendarEventProvider eventProvider = interviewCalendar.getEventProvider();
        eventProvider.removeAllEvents();

        for( IteractionList list : calendarDataDc.getItems() ) {
            Date eventDate = list.getAddDate();

            SimpleCalendarEvent calendarEvent = new SimpleCalendarEvent();
            calendarEvent.setCaption( list.getCandidate().getFullName() );
            calendarEvent.setStart( eventDate );

            // добавим час по умолчанию
            GregorianCalendar calendar = new GregorianCalendar();;

            calendar.setTime( eventDate );
            calendar.add( GregorianCalendar.HOUR, 1 );
            eventDate = calendar.getTime();

            calendarEvent.setEnd( eventDate );

            if( list.getIteractionType().getCalendarItemStyle() != null )
                calendarEvent.setStyleName( list.getIteractionType().getCalendarItemStyle() );

            calendarEvent.setAllDay( true );

            interviewCalendar.getEventProvider().addEvent( calendarEvent );
        }
    }

    @Subscribe("huntingCheckBox")
    public void onHuntingCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if( huntingCheckBox.getValue() ) {
            calendarDataDl.setParameter( "numberInternal", "002" + "%" );
        } else {
            if( !toConpanyCheckBox.getValue() )
                calendarDataDl.setParameter( "numberInternal", "nodata" );
            else
                calendarDataDl.removeParameter( "numberInternal" );
        }

        calendarDataDl.load();

        updateCalendar();
    }

    @Subscribe("toConpanyCheckBox")
    public void onToConpanyCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if( toConpanyCheckBox.getValue() ) {
            calendarDataDl.setParameter( "numberExternal", "003" + "%" );
        } else  {
            if( !huntingCheckBox.getValue() )
                calendarDataDl.setParameter( "numberExternal", "nodata" );
            else
                calendarDataDl.removeParameter( "numberExternal" );
        }

        calendarDataDl.load();

        updateCalendar();
    }

    @Subscribe("monthPicker")
    public void onMonthPickerValueChange(HasValue.ValueChangeEvent<java.sql.Date> event) {
        java.util.Calendar c = new GregorianCalendar();
        c.setTime( event.getValue() );
        c.add( java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
        interviewCalendar.setStartDate((event.getValue()));
        interviewCalendar.setEndDate(c.getTime());

//        reformatCalendar();
    }
}