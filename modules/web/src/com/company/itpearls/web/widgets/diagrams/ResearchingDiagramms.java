package com.company.itpearls.web.widgets.diagrams;

import com.company.itpearls.entity.Iteraction;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.charts.gui.amcharts.model.Color;
import com.haulmont.charts.gui.amcharts.model.Title;
import com.haulmont.charts.gui.components.charts.SerialChart;
import com.haulmont.charts.gui.data.*;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;


// Общий счетчик собеседований
@UiController("itpearls_ResearchingDiagramms")
@UiDescriptor("researching-diagramms.xml")
@DashboardWidget(name = "Диаграмма количества собеседований")
public class ResearchingDiagramms extends ScreenFragment {

    private static final String QUERY_GET_ITERACTIONS =
            "select f from itpearls_Iteraction f where f.iterationName like :iteractionName";

    private static final String QUERY_DAY_COUNT =
            "select count(e) from itpearls_IteractionList e " +
                    "where e.dateIteraction >= :startDate and e.dateIteraction < :endDate " +
                    "and e.iteractionType in :iteractionType";

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
        StringBuilder sb = new StringBuilder();
        sb.append("Количество взаимодействий: ")
                .append(iteractionName);
        titles.add(new Title().setText(sb.toString()).setAlpha(1.0).setColor(Color.BLACK));

        StringBuilder sb1 = new StringBuilder();
        sb1.append(dateFormat.format(startDate))
                .append(" - ")
                .append(dateFormat.format(endDate));
        titles.add(new Title().setText(sb1.toString())
                .setAlpha(1.0).setColor(Color.BROWN).setSize(12));

        makeInterview.setTitles(titles);
    }

    private ListDataProvider valueGraphs() {
        ListDataProvider dataProvider = new ListDataProvider();

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(startDate);

        List<Iteraction> iteraction = loadIteractionTypes(iteractionName + "%");
        List<Iteraction> iteractionsExternalInterview = loadIteractionTypes(externalInterview);
        List<Iteraction> iteractionsInternalInterview = loadIteractionTypes(internalInterview);

        do {
            GregorianCalendar sDate = new GregorianCalendar();
            sDate.setTime(gregorianCalendar.getTime());

            gregorianCalendar.add(Calendar.DAY_OF_MONTH, 1);

            int iteractionCount = countIteractions(iteraction, sDate.getTime(), gregorianCalendar.getTime());
            int internalInterviewCount = countIteractions(iteractionsInternalInterview,
                    sDate.getTime(), gregorianCalendar.getTime());
            int externalInterviewCount = countIteractions(iteractionsExternalInterview,
                    sDate.getTime(), gregorianCalendar.getTime());

            String date = dateFormat.format(sDate.getTime());
            dataProvider.addItem(iteractionCount(date,
                    iteractionCount - internalInterviewCount - externalInterviewCount,
                    internalInterviewCount,
                    externalInterviewCount));
        } while (gregorianCalendar.getTime().before(endDate));

        return dataProvider;
    }

    private List<Iteraction> loadIteractionTypes(String namePattern) {
        return dataManager.load(Iteraction.class)
                .query(QUERY_GET_ITERACTIONS)
                .parameter("iteractionName", namePattern)
                .view("_minimal")
                .cacheable(true)
                .list();
    }

    private DataItem iteractionCount(String date, int allCount, int internalCount, int externalCount) {
        MapDataItem item = new MapDataItem();

        item.add(GRAPH_X, date);
        item.add(GRAPH_Y, allCount);
        item.add(GRAPH_Y_INTERNAL_INTERVIEW, internalCount);
        item.add(GRAPH_Y_EXTERNAL_INTERVIEW, externalCount);

        return item;
    }

    private int countIteractions(List<Iteraction> iteraction, Date startDate, Date endDate) {
        if (iteraction.isEmpty()) {
            return 0;
        }

        Long count = dataManager.loadValue(QUERY_DAY_COUNT, Long.class)
                .parameter("iteractionType", iteraction)
                .parameter("startDate", startDate)
                .parameter("endDate", endDate)
                .one();

        return count != null ? count.intValue() : 0;
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
