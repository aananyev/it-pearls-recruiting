package com.company.itpearls.web.widgets.Diagrams;

import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.google.common.collect.ImmutableMap;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.charts.gui.components.charts.SerialChart;
import com.haulmont.charts.gui.data.DataProvider;
import com.haulmont.charts.gui.data.ListDataProvider;
import com.haulmont.charts.gui.data.MapDataItem;
import com.haulmont.charts.gui.data.SimpleDataItem;
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
import java.util.*;

@UiController("itpearls_ResearchingDiagramms")
@UiDescriptor("researching-diagramms.xml")
@DashboardWidget(name = "Count interview diagramm")
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

    @Inject
    private DataManager dataManager;
    @Inject
    private SerialChart makeInterview;

    private static String GRAPH_X = "date";
    private static String GRAPH_Y = "count";

    @Subscribe
    public void onInit(InitEvent event) {
/*        String queryGraph = "select cast(e.dateIteraction as date), count(e) " +
                "from itpearls_IteractionList e " +
                "where (e.dateIteraction between :startDate and :endDate) and " +
                "e.iteractionType = :iteraction " +
                "group by date(e.dateIteraction) " +
                "order by date(e.dateIteraction)";
*/
        setDeafaultTimeInterval();
        makeInterview.setCategoryField(GRAPH_X);
/*
        Iteraction iteraction = dataManager.load(Iteraction.class)
                .query("select f from itpearls_Iteraction f where f.iterationName = :iteractionName")
                .parameter("iteractionName", iteractionName)
                .view("_minimal")
                .one();

        ValueLoadContext loadContext = ValueLoadContext.create()
                .setQuery(ValueLoadContext.createQuery(queryGraph)
                        .setParameter("iteraction", iteraction)
                        .setParameter("startDate", startDate)
                        .setParameter("endDate", endDate))
                .addProperty(GRAPH_X)
                .addProperty(GRAPH_Y);

        List<KeyValueEntity> iteractionCount = dataManager.loadValues(loadContext);
*/
        makeInterview.setDataProvider(valueGraphs());
    }

    private ListDataProvider valueGraphs() {
        String queryGraph = "select count(e) " +
                "from itpearls_IteractionList e " +
                "where (e.dateIteraction between :startDate and :endDate) and " +
                "e.iteractionType = :iteractionType";
        ListDataProvider dataProvider = new ListDataProvider();

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(startDate);

        Iteraction iteraction = dataManager.load(Iteraction.class)
                .query("select f from itpearls_Iteraction f where f.iterationName = :iteractionName")
                .parameter("iteractionName", iteractionName)
                .view("_minimal")
                .one();

        do {

            GregorianCalendar sDate = new GregorianCalendar();
            sDate.setTime(gregorianCalendar.getTime());

            gregorianCalendar.add(Calendar.DAY_OF_MONTH, 1);

            ValueLoadContext loadContext = ValueLoadContext.create()
                    .setQuery(ValueLoadContext.createQuery(queryGraph)
                            .setParameter("iteractionType", iteraction)
                            .setParameter("startDate", sDate.getTime())
                            .setParameter("endDate", gregorianCalendar.getTime()))
                    .addProperty(GRAPH_Y);

            List<KeyValueEntity> iteractionCount = dataManager.loadValues(loadContext);

            Date date = sDate.getTime();
            int count = iteractionCount.size();
            dataProvider.addItem(new MapDataItem(ImmutableMap.of(GRAPH_Y, count, GRAPH_X, date)));
        } while (gregorianCalendar.getTime().before(endDate));

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
}