package com.company.itpearls.web.widgets.reports;

import com.company.itpearls.entity.IteractionList;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@UiController("itpearls_LastStatusCandidateToDate")
@UiDescriptor("last-status-candidate-to-date.xml")
@DashboardWidget(name = "Last Status")
public class LastStatusCandidateToDate extends ScreenFragment {
    @WidgetParam
    @WindowParam
    protected Date startDate;

    @WidgetParam
    @WindowParam
    protected Date endDate;


    @WidgetParam
    @WindowParam
    protected String iteractionName;

    @Inject
    private Label<String> widgetTitle;
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSession userSession;


    List<KeyValueEntity> iteractionList = new ArrayList<>();

    @Subscribe
    public void onInit(InitEvent event) {
        setDeafaultTimeInterval();
        setWidgetTitle();
        setIteractionList();
    }

    private void setIteractionList() {
        String query = "select e from IteractionList e " +
                "where e.dateIteraction between :startDate and :endDate and " +
                "e.recrutier = :recrutier and " +
                "e.iteractionType = (select f from itpearls_Iteraction f where f.iterationName like :iteractionName )";

        iteractionList = dataManager.loadValues(query)
                .parameter("startDate", startDate)
                .parameter("endDate", endDate)
                .parameter("recrutier", userSession.getUser())
                .parameter(iteractionName, iteractionName)
                .list();

        setList(iteractionList);
    }

    private void setList(List<KeyValueEntity> iteractionList) {
        for(KeyValueEntity a : iteractionList) {

        }
    }

    private void setDeafaultTimeInterval() {
        if (startDate == null || endDate == null) {
            GregorianCalendar calendar = new GregorianCalendar();

            calendar.set(GregorianCalendar.DATE, 1);
            startDate = calendar.getTime();

            calendar.set(GregorianCalendar.DAY_OF_MONTH, calendar.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
            endDate = calendar.getTime();
        }
    }

    private void setWidgetTitle() {
        String title = "Напоминание (" + iteractionName + "):";
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");

        widgetTitle.setValue(title + df.format(startDate) + " - " + df.format(endDate));
    }

}