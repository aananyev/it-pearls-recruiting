package com.company.itpearls.web.widgets.diagrams;

import com.company.itpearls.entity.IteractionList;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.charts.gui.amcharts.model.GaugeArrow;
import com.haulmont.charts.gui.components.charts.AngularGaugeChart;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@UiController("itpearls_InterviewsPerMonthsWidget")
@UiDescriptor("interviews-per-months-widget.xml")
@DashboardWidget(name="Interview per months")
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

    private void setAssignedInterviewToday() {
        final String query = "select e from itpearls_IteractionList e " +
                "where e.iteractionType.signOurInterviewAssigned = true and e.recrutier = :recrutier " +
                "and e.dateIteraction between :startDate and :endDate";

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.set(GregorianCalendar.DAY_OF_MONTH, 1);

        List<GaugeArrow> arrows = new ArrayList();
        arrows.add(new GaugeArrow().setValue(Double.valueOf(dataManager
                .load(IteractionList.class)
                .query(query)
                .parameter("recrutier", userSession.getUser())
                .parameter("endDate", new Date())
                .parameter("startDate", gregorianCalendar.getTime())
                .view("iteractionList-view")
                .list()
                .size())));

        gaugeChart.setArrows(arrows);
/*                dataManager
                        .load(IteractionList.class)
                        .query(query)
                        .parameter("recrutier", userSession.getUser())
                        .view("iteractionList-view")
                        .list()
                        .size())); */
    }
}