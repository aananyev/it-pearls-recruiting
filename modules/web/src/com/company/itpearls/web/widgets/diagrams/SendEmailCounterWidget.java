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

@UiController("itpearls_SendEmailCounterWidget")
@UiDescriptor("send-email-counter-widget.xml")
@DashboardWidget(name="Send email counter")
public class SendEmailCounterWidget extends ScreenFragment {

    @Inject
    private DataManager dataManager;
    @Inject
    private UserSession userSession;
    @Inject
    private AngularGaugeChart gaugeChart;

    @Subscribe
    public void onInit(InitEvent event) {
        setAssignedInterviewToday();
    }

    private void setAssignedInterviewToday() {
        final String query = "select e from itpearls_IteractionList e " +
                "where e.iteractionType.signEmailSend = true and e.recrutier = :recrutier " +
                "and e.dateIteraction between :startDate and :endDate";

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.add(GregorianCalendar.DAY_OF_MONTH, -1);

        List<GaugeArrow> arrows = new ArrayList();
        Double arrowData = Double.valueOf(dataManager
                .load(IteractionList.class)
                .query(query)
                .parameter("recrutier", userSession.getUser())
                .parameter("endDate", new Date())
                .parameter("startDate", gregorianCalendar.getTime())
                .view("iteractionList-view")
                .list()
                .size());

        if (arrowData > gaugeChart.getAxes().get(0).getEndValue()) {
            gaugeChart.getAxes().get(0).setEndValue((arrowData / 10 + 1) * 10);
        }

        arrows.add(new GaugeArrow().setValue(arrowData));

        gaugeChart.setArrows(arrows);
    }
}