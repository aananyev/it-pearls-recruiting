package com.company.itpearls.web.widgets;

import com.company.itpearls.entity.IteractionList;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.gui.components.Calendar;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.calendar.SimpleCalendarEvent;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.Date;
import java.util.GregorianCalendar;

@UiController("itpearls_InterviewCalendarWidget")
@UiDescriptor("interview-calendar-widget.xml")
@DashboardWidget( name = "Interview Calendar" )
public class InterviewCalendarWidget extends ScreenFragment {
    @Inject
    private Calendar<Date> interviewCalendar;
    @Inject
    private CollectionContainer<IteractionList> calendarDataDc;
    @Inject
    private CollectionLoader<IteractionList> calendarDataDl;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        calendarDataDl.load();

        for( IteractionList list : calendarDataDc.getItems() ) {
            SimpleCalendarEvent calendarEvent = new SimpleCalendarEvent();
            calendarEvent.setCaption( list.getCandidate().getFullName() );
            calendarEvent.setStart( list.getAddDate() );
            calendarEvent.setEnd( list.getAddDate() );
            calendarEvent.setAllDay( true );

            interviewCalendar.getEventProvider().addEvent( calendarEvent );
        }

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