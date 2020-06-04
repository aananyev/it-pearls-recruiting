package com.company.itpearls.web.widgets.Diagrams;

import com.company.itpearls.entity.IteractionList;
import com.google.common.collect.ImmutableMap;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.charts.gui.components.charts.SerialChart;
import com.haulmont.charts.gui.data.DataItem;
import com.haulmont.charts.gui.data.ListDataProvider;
import com.haulmont.charts.gui.data.MapDataItem;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import java.util.*;

@UiController("itpearls_ResearcherDiagramWidget")
@UiDescriptor("researcher-repost-diagram-widget.xml")
@DashboardWidget(name = "Researcher Diagram Widget")
public class ResearcherDiagramWidget extends ScreenFragment {

    @WidgetParam
    @WindowParam
    protected Date startDate;

    private List<IteractionList> iteractionList = new ArrayList<>();

    @WidgetParam
    @WindowParam
    protected Date endDate;
    @Inject
    private DataManager dataManager;
    @Inject
    private SerialChart makeInterview;

    @Subscribe
    public void onInit(InitEvent event) {
        setDeafaultTimeInterval();
        setValueDiagramData();
    }

    private void setValueDiagramData() {
        String QUERY_FOR_DIAGRAMM = "select f " +
                "from itpearls_IteractionList f " +
                "where f.dateIteraction between :startDate and :endDate " +
                "order by f.dateIteraction";

        ListDataProvider dataProvider = new ListDataProvider();

/*
        iteractionList = dataManager.load(IteractionList.class)
                .query(QUERY_FOR_DIAGRAMM)
                .parameter("startDate", startDate)
                .parameter("endDate", endDate)
                .list(); */

        Date d = startDate;

        do {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(d);
            calendar.add(Calendar.DAY_OF_MONTH,1);

            Date endDay = calendar.getTime();

            LoadContext<IteractionList> loadContext = LoadContext.create(IteractionList.class)
                    .setQuery(LoadContext.createQuery(QUERY_FOR_DIAGRAMM)
                            .setParameter("startDate", d)
                            .setParameter("endDate", endDay))
                    .setView("iteractionList-view");

            d = calendar.getTime();

            iteractionList = dataManager.loadList(loadContext);

            Integer iteractionCount = iteractionList.size();

            dataProvider.addItem(new MapDataItem(ImmutableMap.of("date", d, "count", iteractionCount)));
        } while (d.before(endDate));

        makeInterview.setDataProvider(dataProvider);
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