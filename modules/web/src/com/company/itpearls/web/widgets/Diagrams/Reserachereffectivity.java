package com.company.itpearls.web.widgets.Diagrams;

import com.company.itpearls.entity.IteractionList;
import com.google.common.collect.ImmutableMap;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.charts.gui.amcharts.model.Color;
import com.haulmont.charts.gui.amcharts.model.Title;
import com.haulmont.charts.gui.components.charts.SerialChart;
import com.haulmont.charts.gui.data.DataProvider;
import com.haulmont.charts.gui.data.ListDataProvider;
import com.haulmont.charts.gui.data.MapDataItem;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@UiController("itpearls_Reserachereffectivity")
@UiDescriptor("ReseracherEffectivity.xml")
@DashboardWidget(name = "Reserarcher effectivity") // ++
public class Reserachereffectivity extends ScreenFragment {

    @WidgetParam
    @WindowParam
    protected Date startDate;

    @WidgetParam
    @WindowParam
    protected Date endDate;

    @WidgetParam
    @WindowParam
    protected String iteractionName;

    @WidgetParam
    @WindowParam
    protected String userRole;

    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private static String GRAPH_X = "recrutier";
    private static String GRAPH_Y = "count";

    @Inject
    private SerialChart countIteractionChart;
    @Inject
    private DataManager dataManager;

    @Subscribe
    public void onInit(InitEvent event) {
        setDeafaultTimeInterval();

        countIteractionChart.setCategoryField(GRAPH_X);
        countIteractionChart.setDataProvider(valueGraphs());

        setDiagramTitle();
    }

    private DataProvider valueGraphs() {
        ListDataProvider dataProvider = new ListDataProvider();
        String queryCount;

        if (iteractionName == null) {
            queryCount = "select e from itpearls_IteractionList e " +
                    "where e.recrutier = :user  and " +
                    "(e.dateIteraction between :startDate and :endDate) " +
                    "order by e.recrutier.name";
        } else {
            queryCount = "select e from itpearls_IteractionList e " +
                    "where e.recrutier = :user  and " +
                    "(e.dateIteraction between :startDate and :endDate) and " +
                    "e.iteractionType = (select f from itpearls_Iteraction f where f.iterationName like :iteractionName)" +
                    "order by e.recrutier.name";
        }

        List<User> users = dataManager.load(User.class).list();

        for (User a : users) {
            if (a.getActive() & a.getFirstName() != null & a.getLastName() != null) {
                int count;
                if (iteractionName == null) {
                    count = dataManager.load(IteractionList.class)
                            .query(queryCount)
                            .view("iteractionList-view")
                            .parameter("user", a)
                            .parameter("startDate", startDate)
                            .parameter("endDate", endDate)
                            .list()
                            .size();
                } else {
                    count = dataManager.load(IteractionList.class)
                            .query(queryCount)
                            .view("iteractionList-view")
                            .parameter("user", a)
                            .parameter("iteractionName", iteractionName)
                            .parameter("startDate", startDate)
                            .parameter("endDate", endDate)
                            .list()
                            .size();
                }

                dataProvider.addItem(new MapDataItem(ImmutableMap.of(GRAPH_Y, count, GRAPH_X,
                        a.getFirstName() + "\n" + a.getLastName())));

            }
        }

        return dataProvider;

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

    private void setDiagramTitle() {
        iteractionName = iteractionName == null ? "" : iteractionName;

        List<Title> titles = new ArrayList<>();
        titles.add(new Title()
                .setText("Взаимодействия рекрутеров" +
                        (iteractionName != null ? ": " : "") + iteractionName)
                .setAlpha(1.0)
                .setColor(Color.BLACK));
        titles.add(new Title().setText(dateFormat.format(startDate) + " - " +
                dateFormat.format(endDate)).setAlpha(1.0).setColor(Color.BROWN).setSize(12));

        countIteractionChart.setTitles(titles);
    }
}