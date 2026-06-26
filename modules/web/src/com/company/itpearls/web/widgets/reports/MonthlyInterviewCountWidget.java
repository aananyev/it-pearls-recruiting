/*
Это виджет прямоугольный статистика агрегированная
*/
package com.company.itpearls.web.widgets.reports;

import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.*;

@UiController("itpearls_MonthlyInterviewCountWidget")
@UiDescriptor("monthly-interview-count-widget.xml")
@DashboardWidget(name = "Количество взаимодействий за месяц")
public class MonthlyInterviewCountWidget extends ScreenFragment {

    private static final String QUERY_INTERACTION_COUNT =
            "select count(e) from itpearls_IteractionList e " +
                    "where e.iteractionType.iterationName like :iteractionName " +
                    "and e.dateIteraction between :startDate and :endDate";

    @Inject
    private Label<String> labelAssignExternalInterview;
    @Inject
    private Label<String> labelCountNewContacts;
    @Inject
    private Label<String> labelPrepareExternalInterview;
    @Inject
    private Label<String> labelPrepareInternalInterview;
    @Inject
    private Label<String> labelWidgetTitle;
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
    private Label<String> labelDirectorsInterview;
    @Inject
    private DataManager dataManager;

    @WidgetParam
    @WindowParam
    protected Date startDate;

    @WidgetParam
    @WindowParam
    protected Date endDate;

    @WidgetParam
    @WindowParam
    protected String widgetTitle;

    @WidgetParam
    @WindowParam
    protected String period;

    private String itemNewContact = "Новый контакт";
    private String itemProposeJob = "Предложение работы";
    private String itemAssignInternalInterview = "Назначено собеседование с рекрутером IT Pearls";
    private String itemPrepareInternalInterview = "Прошел собеседование с рекрутером IT Pearls";
    private String itemAssignExternalInterview = "Назначено техническое собеседование";
    private String itemPrepareExternalInterview = "Прошел техническое собеседование";
    private String itemPrepareDirectorsInterview = "Прошел собеседование с Директором";
    @Inject
    private Label<String> labelCountProposeJob;
    @Inject
    private Label<String> labelCountProposeJobValue;
    @Inject
    private Label<String> labelAssignInternalInterview;
    @Inject
    private Label<String> labelAssignInternalInterviewValue;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        labelWidgetTitle.setValue(widgetTitle);

        setDateInterval();
        setLabelNewContacts();
        setLabelProposeJob();
        setLabelAssignInternalInterview();
        setLabelPrepareInternalInterview();
        setLabelAssignExternalInterview();
        setLabelPrepareExternalInterview();
        setLabelPrepareDirectorsInterview();
    }

    private void setLabelProposeJob() {
        int count = countInteractions(itemProposeJob + "%");
        labelCountProposeJob.setValue(itemProposeJob);
        labelCountProposeJobValue.setValue(String.valueOf(count));
    }

    private void setLabelPrepareDirectorsInterview() {
        int count = countInteractions(itemPrepareDirectorsInterview + "%");
        labelDirectorsInterview.setValue(itemPrepareDirectorsInterview);
        labelDirectorsInterviewValue.setValue(String.valueOf(count));
    }

    private void setLabelPrepareExternalInterview() {
        int count = countInteractions(itemPrepareExternalInterview + "%");
        labelPrepareExternalInterview.setValue(itemPrepareExternalInterview);
        labelPrepareExternalInterviewValue.setValue(String.valueOf(count));
    }

    private void setLabelAssignExternalInterview() {
        int count = countInteractions(itemAssignExternalInterview + "%");
        labelAssignExternalInterview.setValue(itemAssignExternalInterview);
        labelAssignExternalInterviewValue.setValue(String.valueOf(count));
    }

    private void setLabelPrepareInternalInterview() {
        int count = countInteractions(itemPrepareInternalInterview + "%");
        labelPrepareInternalInterview.setValue(itemPrepareInternalInterview);
        labelPrepareInternalInterviewValus.setValue(String.valueOf(count));
    }

    private void setLabelNewContacts() {
        int countItems = countInteractions(itemNewContact + "%");
        labelCountNewContacts.setValue(itemNewContact);
        labelCountNewContactsValue.setValue(String.valueOf(countItems));
    }

    public static int getCurrentYear() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
        calendar.setTime(new Date());
        return calendar.get(Calendar.YEAR);
    }

    public static int getCurrentMonth() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
        calendar.setTime(new Date());
        return calendar.get(Calendar.MONTH);
    }

    private void setDateInterval() {
        Integer endDateOfPeriod = Calendar.DAY_OF_MONTH;
        Calendar firstDay = new GregorianCalendar();

        if (startDate == null) {
            firstDay = new GregorianCalendar(getCurrentYear(), getCurrentMonth(), 1);
            startDate = firstDay.getTime();
        }

        if (endDate == null) {
            Calendar endDayCal = new GregorianCalendar(getCurrentYear(),
                    getCurrentMonth(),
                    firstDay.getActualMaximum(endDateOfPeriod));
            endDate = endDayCal.getTime();
        }
    }

    private void setLabelAssignInternalInterview() {
        int count = countInteractions(itemAssignInternalInterview + "%");
        labelAssignInternalInterview.setValue(itemAssignInternalInterview);
        labelAssignInternalInterviewValue.setValue(String.valueOf(count));
    }

    private int countInteractions(String iteractionNamePattern) {
        Long count = dataManager.loadValue(QUERY_INTERACTION_COUNT, Long.class)
                .parameter("iteractionName", iteractionNamePattern)
                .parameter("startDate", startDate)
                .parameter("endDate", endDate)
                .one();
        return count != null ? count.intValue() : 0;
    }
}
