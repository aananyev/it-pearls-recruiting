package com.company.itpearls.web.screens.interviewcalendar;

import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.calendar.CalendarEventProvider;
import com.haulmont.cuba.gui.components.calendar.ListCalendarEventProvider;
import com.haulmont.cuba.gui.components.calendar.SimpleCalendarEvent;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_Interviewcalendar")
@UiDescriptor("interviewCalendar.xml")
public class Interviewcalendar extends Screen {
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

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        huntingCheckBox.setValue( true );
        toConpanyCheckBox.setValue( true );

        calendarDataDl.load();

        createTypeCalendarRadioButton();

        updateCalendar();
    }

    private void createTypeCalendarRadioButton() {
        Map<String, Integer> typeCalendarRadioButton = new LinkedHashMap<>();

        typeCalendarRadioButton.put( "День", 1 );
        typeCalendarRadioButton.put( "Неделя", 2 );
        typeCalendarRadioButton.put( "Месяц", 3 );

        typeCalendar.setOptionsMap( typeCalendarRadioButton );
    }

    @Subscribe("typeCalendar")
    public void onTypeCalendarValueChange(HasValue.ValueChangeEvent event) {
        Date currentDate = new Date();

        if( typeCalendar.getValue().equals( 1 ) ) {
            interviewCalendar.setStartDate( currentDate );
            interviewCalendar.setEndDate( currentDate );
        }

        if( typeCalendar.getValue().equals( 2 ) ) {
            java.util.Calendar c = new GregorianCalendar();
            c.setTime( currentDate );
            c.add( java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_WEEK));
            interviewCalendar.setEndDate(c.getTime());

            c.add( java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_WEEK));
            interviewCalendar.setStartDate( c.getTime() );
        }

        if( typeCalendar.getValue().equals( 3 ) ) {
            java.util.Calendar c = new GregorianCalendar();
            c.setTime( currentDate );
            c.add( java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
            interviewCalendar.setEndDate(c.getTime());

            c.add( java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
            interviewCalendar.setStartDate( c.getTime() );
        }

        updateCalendar();
    }



    private void updateCalendar() {
        CalendarEventProvider eventProvider = interviewCalendar.getEventProvider();
        eventProvider.removeAllEvents();

        for( IteractionList list : calendarDataDc.getItems() ) {
            SimpleCalendarEvent calendarEvent = new SimpleCalendarEvent();
            calendarEvent.setCaption( list.getCandidate().getFullName() );
            calendarEvent.setStart( list.getAddDate() );
            calendarEvent.setEnd( list.getAddDate() );

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
    }
}