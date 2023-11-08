package com.company.itpearls.web.widgets.diagrams;

import com.company.itpearls.entity.IteractionList;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.charts.gui.amcharts.model.GaugeArrow;
import com.haulmont.charts.gui.components.charts.AngularGaugeChart;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.AppUI;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

@UiController("itpearls_InterviewsPerMonthsWidget")
@UiDescriptor("interviews-per-months-widget.xml")
@DashboardWidget(name = "Interview per months assigned")
public class InterviewsPerMonthsWidget extends ScreenFragment {
    @Inject
    private AngularGaugeChart gaugeChart;
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSession userSession;

    @Subscribe
    public void onInit(InitEvent event) {
        setAssignedInterviewToday();
    }

    final static String query = "select e from itpearls_IteractionList e " +
            "where e.iteractionType.signOurInterviewAssigned = true and e.recrutier = :recrutier " +
            "and e.dateIteraction between :startDate and :endDate";

    private void setAssignedInterviewToday() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.set(GregorianCalendar.DAY_OF_MONTH, 1);

        List<GaugeArrow> arrows = new ArrayList();

        Double arrowData = Double.valueOf(dataManager
                .load(IteractionList.class)
                .query(query)
                .parameter("recrutier", userSession.getUser())
                .parameter("endDate", new Date())
                .parameter("startDate", gregorianCalendar.getTime())
                .view("iteractionList-view")
                .cacheable(true)
                .list()
                .size());

        if (arrowData > gaugeChart.getAxes().get(0).getEndValue()) {
            double endValue = (arrowData / 10 + 1) / 10;
            gaugeChart.getAxes().get(0).setEndValue(endValue);
        }

        arrows.add(new GaugeArrow().setValue(arrowData));
        gaugeChart.setArrows(arrows);
    }
}