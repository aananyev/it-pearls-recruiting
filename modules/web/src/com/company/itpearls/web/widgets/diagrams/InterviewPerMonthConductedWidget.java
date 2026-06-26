package com.company.itpearls.web.widgets.diagrams;

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

@UiController("itpearls_InterviewPerMonthConductedWidget")
@UiDescriptor("interview-per-month-conducted-widget.xml")
@DashboardWidget(name = "Собеседования в месяц (проведённые)")
public class InterviewPerMonthConductedWidget extends ScreenFragment {
    @Inject
    private AngularGaugeChart gaugeChart;
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSession userSession;

    private static final String QUERY_CONDUCTED_COUNT =
            "select count(e) from itpearls_IteractionList e " +
                    "where e.iteractionType.signOurInterview = true and e.recrutier = :recrutier " +
                    "and e.dateIteraction between :startDate and :endDate";

    @Subscribe
    public void onInit(InitEvent event) {
        setAssignedInterviewToday();
    }

    private void setAssignedInterviewToday() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.set(GregorianCalendar.DAY_OF_MONTH, 1);

        List<GaugeArrow> arrows = new ArrayList();

        Long count = dataManager.loadValue(QUERY_CONDUCTED_COUNT, Long.class)
                .parameter("recrutier", userSession.getUser())
                .parameter("endDate", new Date())
                .parameter("startDate", gregorianCalendar.getTime())
                .one();
        Double arrowData = count != null ? count.doubleValue() : 0.0;

        if (arrowData > gaugeChart.getAxes().get(0).getEndValue()) {
            double endValue = (arrowData / 10 + 1) / 10;
            gaugeChart.getAxes().get(0).setEndValue(endValue);
        }

        arrows.add(new GaugeArrow().setValue(arrowData));
        gaugeChart.setArrows(arrows);
    }
}