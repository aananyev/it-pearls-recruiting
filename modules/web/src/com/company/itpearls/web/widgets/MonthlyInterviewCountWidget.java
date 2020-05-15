package com.company.itpearls.web.widgets;

import com.company.itpearls.entity.IteractionList;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@UiController("itpearls_MonthlyInterviewCountWidget")
@UiDescriptor("monthly-interview-count-widget.xml")
@DashboardWidget(name = "Mountly Iteraction Count")
public class MonthlyInterviewCountWidget extends ScreenFragment {
    @Inject
    private Label<String> labelAssignExternalInterview;
    @Inject
    private Label<String> labelAssignInternalInterview;
    @Inject
    private Label<String> labelCountNewContacts;
    @Inject
    private Label<String> labelPrepareExternalInterview;
    @Inject
    private Label<String> labelPrepareInternalInterview;
    @Inject
    private Label<String> labelTitle;
    @Inject
    private CollectionLoader<IteractionList> iteractioListDl;
    @Inject
    private CollectionContainer<IteractionList> iteractionListDc;
    @Inject
    private Label<String> labelAssignExternalInterviewValue;
    @Inject
    private Label<String> labelPrepareInternalInterviewValus;
    @Inject
    private Label<String> labelPrepareExternalInterviewValue;
    @Inject
    private Label<String> labelDirectorsInterviewValue;
    @Inject
    private Label<String> labelCountNewContactsValue;
    @Inject
    private Label<String> labelAssignInternalInterviewValue;
    @Inject
    private Label<String> labelDirectorsInterview;

    @WidgetParam
    @WindowParam
    protected Integer monthNumber;

    @WidgetParam
    @WindowParam
    protected Integer yearNumber;

    @WidgetParam
    @WindowParam
    protected String monthName;

    @WidgetParam
    @WindowParam
    protected String period;

    private String itemNewContact = "Новый контакт";
    private String itemAssignInternalInterview = "Назначено собеседование с рекрутером IT Pearls";
    private String itemPrepareInternalInterview = "Прошел собеседование с рекрутером IT Pearls";
    private String itemAssignExternalInterview = "Назначено техническое собеседование";
    private String itemPrepareExternalInterview = "Прошел техническое собеседование";
    private String itemPrepareDirectorsInterview = "Прошел собеседование с Директором";

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        labelTitle.setValue( "<b><u>Взаимодействия за " +
                ( monthName != null ? monthName : "текущий" ) +
                " месяц</u></b>" );

        setLabelNewContacts();
        setLabelAssignInternalInterview();
        setLabelPrepareInternalInterview();
        setLabelAssignExternalInterview();
        setLabelPrepareExternalInterview();
        setLabelPrepareDirectorsInterview();
    }

    private void setLabelPrepareDirectorsInterview() {
        iteractioListDl.setParameter( "iteractionName", itemPrepareDirectorsInterview + "%" );
        iteractioListDl.load();

        int count = iteractionListDc.getItems().size();
        labelDirectorsInterview.setValue( itemPrepareDirectorsInterview );
        labelDirectorsInterviewValue.setValue( String.valueOf( count ) );
    }

    private void setLabelPrepareExternalInterview() {
        iteractioListDl.setParameter( "iteractionName", itemPrepareExternalInterview + "%" );
        iteractioListDl.load();

        int count = iteractionListDc.getItems().size();
        labelPrepareExternalInterview.setValue( itemPrepareExternalInterview );
        labelPrepareExternalInterviewValue.setValue( String.valueOf( count ));
    }

    private void setLabelAssignExternalInterview() {
        iteractioListDl.setParameter( "iteractionName", itemAssignExternalInterview + "%" );
        iteractioListDl.load();

        int count = iteractionListDc.getItems().size();
        labelAssignExternalInterview.setValue( itemAssignExternalInterview );
        labelAssignExternalInterviewValue.setValue( String.valueOf( count ));
    }

    private void setLabelPrepareInternalInterview() {
        iteractioListDl.setParameter( "iteractionName", itemPrepareInternalInterview + "%" );
        iteractioListDl.load();

        int count = iteractionListDc.getItems().size();
        labelPrepareInternalInterview.setValue( itemPrepareInternalInterview );
        labelPrepareInternalInterviewValus.setValue( String.valueOf( count ) );
    }


    private void setLabelNewContacts() {
        setDateInterval();

        iteractioListDl.setParameter( "iteractionName", itemNewContact + "%" );
        iteractioListDl.load();

        int countItems = 0;
        countItems = iteractionListDc.getItems().size();

        labelCountNewContacts.setValue( itemNewContact );
        labelCountNewContactsValue.setValue( String.valueOf( countItems ) );
    }

    public static int getCurrentYear()
    {
        java.util.Calendar calendar = java.util.Calendar.getInstance(java.util.TimeZone.getDefault(), java.util.Locale.getDefault());
        calendar.setTime(new java.util.Date());
        return calendar.get(java.util.Calendar.YEAR);
    }


    public static int getCurrentMonth()
    {
        java.util.Calendar calendar = java.util.Calendar.getInstance(java.util.TimeZone.getDefault(), java.util.Locale.getDefault());
        calendar.setTime(new java.util.Date());
        return calendar.get(Calendar.MONTH);
    }

    private void setDateInterval() {

        Calendar firstDayCal = new GregorianCalendar( yearNumber != null ? yearNumber : getCurrentYear(),
                    monthNumber != null ? monthNumber - 1 : getCurrentMonth(), 1);

        Date firstDay = firstDayCal.getTime();

        Calendar endDayCal = new GregorianCalendar( yearNumber != null ? yearNumber : getCurrentYear(),
                monthNumber != null ? monthNumber - 1 : getCurrentMonth(),
                firstDayCal.getActualMaximum( Calendar.DAY_OF_MONTH ));

        Date endDay = endDayCal.getTime();

        iteractioListDl.setParameter( "startDate", firstDay );
        iteractioListDl.setParameter( "endDate", endDay );

        iteractioListDl.load();
    }

    private void setLabelAssignInternalInterview() {
        iteractioListDl.setParameter( "iteractionName", itemAssignInternalInterview + "%" );
        iteractioListDl.load();

        int count = iteractionListDc.getItems().size();
        labelAssignInternalInterview.setValue( itemAssignInternalInterview );
        labelAssignInternalInterviewValue.setValue( String.valueOf( count ) );
    }
}