package com.company.itpearls.web.widgets.Diagrams;

import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.google.common.collect.ImmutableMap;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.charts.gui.amcharts.model.Color;
import com.haulmont.charts.gui.amcharts.model.Title;
import com.haulmont.charts.gui.components.charts.SerialChart;
import com.haulmont.charts.gui.data.*;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.ValueLoadContext;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import jdk.jfr.ValueDescriptor;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;


// Общий счетчик собеседований
@UiController("itpearls_ResearchingDiagramms")
@UiDescriptor("researching-diagramms.xml")
@DashboardWidget(name = "Count interview diagramm") // ++
public class ResearchingDiagramms extends ScreenFragment {

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
    protected String internalInterview = "Назначено собеседование с рекрутером IT Pearls";

    @WidgetParam
    @WindowParam
    protected String externalInterview = "Назначено техническое собеседование";

    @WidgetParam
    @WindowParam
    protected String baloonColor;

    @Inject
    private DataManager dataManager;
    @Inject
    private SerialChart makeInterview;

    private static String GRAPH_X = "date";
    private static String GRAPH_Y = "count";
    private static String GRAPH_Y_EXTERNAL_INTERVIEW = "externalInterview";
    private static String GRAPH_Y_INTERNAL_INTERVIEW = "internalInterview";
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    @Subscribe
    public void onInit(InitEvent event) {
        setDeafaultTimeInterval();
        makeInterview.setCategoryField(GRAPH_X);
        makeInterview.setDataProvider(valueGraphs());

        setDiagramTitle();
    }

    private void setDiagramTitle() {
        List<Title> titles = new ArrayList<>();
        titles.add(new Title().setText("Количество взаимодействий: " + iteractionName).setAlpha(1.0).setColor(Color.BLACK));
        titles.add(new Title().setText(dateFormat.format(startDate) + " - " +
                dateFormat.format(endDate)).setAlpha(1.0).setColor(Color.BROWN).setSize(12));

        makeInterview.setTitles(titles);
    }

    private ListDataProvider valueGraphs() {

        String QUERY_GET_ITERACTIONS = "select f from itpearls_Iteraction f where f.iterationName like :iteractionName";
        ListDataProvider dataProvider = new ListDataProvider();

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(startDate);

        List<Iteraction> iteraction = dataManager.load(Iteraction.class)
                .query(QUERY_GET_ITERACTIONS)
                .parameter("iteractionName", iteractionName + "%")
                .view("_minimal")
                .list();
        List<Iteraction> iteractionsExternalInterview = dataManager.load(Iteraction.class)
                .query(QUERY_GET_ITERACTIONS)
                .parameter("iteractionName", externalInterview)
                .view("_minimal")
                .list();
        List<Iteraction> iteractionsInternalInterview = dataManager.load(Iteraction.class)
                .query(QUERY_GET_ITERACTIONS)
                .parameter("iteractionName", internalInterview)
                .view("_minimal")
                .list();

        do {
            GregorianCalendar sDate = new GregorianCalendar();
            sDate.setTime(gregorianCalendar.getTime());

            gregorianCalendar.add(Calendar.DAY_OF_MONTH, 1);

            int iteractionCount = getCountIteraction(iteraction,
                    sDate.getTime(), gregorianCalendar.getTime(),
                    GRAPH_Y);
            int internalInterviewCount = getCountIteraction(iteractionsInternalInterview,
                    sDate.getTime(), gregorianCalendar.getTime(),
                    GRAPH_Y_INTERNAL_INTERVIEW);
            int externalInterviewCount = getCountIteraction(iteractionsExternalInterview,
                    sDate.getTime(), gregorianCalendar.getTime(),
                    GRAPH_Y_EXTERNAL_INTERVIEW);

            String date = dateFormat.format(sDate.getTime());
            dataProvider.addItem(iteractionCount(date,
                    iteractionCount - internalInterviewCount - externalInterviewCount,
                    internalInterviewCount,
                    externalInterviewCount));
        } while (gregorianCalendar.getTime().before(endDate));

        return dataProvider;
    }

    private DataItem iteractionCount(String date, int allCount, int internalCount, int externalCount) {
        MapDataItem item = new MapDataItem();

        item.add(GRAPH_X, date);
        item.add(GRAPH_Y, allCount);
        item.add(GRAPH_Y_INTERNAL_INTERVIEW, internalCount);
        item.add(GRAPH_Y_EXTERNAL_INTERVIEW, externalCount);

        return item;
    }

    private int getCountIteraction(List<Iteraction> iteraction, Date startDate, Date endDate, String property) {
        String queryGraph = "select e " +
                "from itpearls_IteractionList e " +
                "where (e.dateIteraction between :startDate and :endDate) and " +
                "e.iteractionType in :iteractionType";

        List<IteractionList> iteractionLists = new ArrayList<>();

         int retInt = 0;
         try {
             iteractionLists = dataManager.load(IteractionList.class)
                     .query(queryGraph)
                     .view("iteractionList-view")
                     .parameter("iteractionType", iteraction)
                     .parameter("startDate", startDate)
                     .parameter("endDate", endDate)
                     .list();

             retInt = iteractionLists.size();
         } catch (Exception e) {
             retInt = 0;
         }

         return retInt;
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
}