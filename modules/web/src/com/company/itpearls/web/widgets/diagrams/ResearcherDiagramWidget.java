package com.company.itpearls.web.widgets.diagrams;

import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.charts.gui.amcharts.model.GraphType;
import com.haulmont.charts.gui.components.charts.SerialChart;
import com.haulmont.charts.gui.data.ListDataProvider;
import com.haulmont.charts.gui.data.MapDataItem;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.*;

@UiController("itpearls_ResearcherDiagramWidget")
@UiDescriptor("researcher-repost-diagram-widget.xml")
@DashboardWidget(name = "Диаграмма ресерчера")
public class ResearcherDiagramWidget extends ScreenFragment {

    private static final String QUERY_DAY_COUNT_BY_TYPE =
            "select count(f) from itpearls_IteractionList f " +
                    "where f.dateIteraction >= :startDate and f.dateIteraction < :endDate and " +
                    "f.iteractionType = (select e from itpearls_Iteraction e where e.iterationName like :iteractionName)";

    private static final String ITRKT_NEW_CONTACT = "Новый контакт";
    private static final String ITRKT_PROPOSE_JOB = "Предложение работы";
    private static final String ITRKT_ASSIGN_ITPEARKS_INTERVIEW = "Назначено собеседование с рекрутером IT Pearls";
    private static final String ITRKT_PREPARE_ITPEARKS_INTERVIEW = "Прошел собеседование с рекрутером IT Pearls";
    private static final String ITRKT_ASSIGN_TECH_INTERVIEW = "Назначено техническое собеседование";
    private static final String ITRKT_PREPARE_TECH_INTERVIEW = "Прошел техническое собеседование";
    private static final String ITRKT_PREPARE_DIRECTOR_INTERVIEW = "Прошел собеседование с Директором";

    private final List<GraphTable> tableGraph = new ArrayList<>();

    @WidgetParam
    @WindowParam
    protected Date startDate;

    @WidgetParam
    @WindowParam
    protected Date endDate;

    @Inject
    private DataManager dataManager;
    @Inject
    private SerialChart makeInterview;

    @Subscribe
    public void onInit(InitEvent event) {
        setInitHasTable();
        setDeafaultTimeInterval();

        setGraphs();
        setValueDiagramData();
    }

    private void setGraphs() {
        for (GraphTable a : tableGraph) {
            com.haulmont.charts.gui.amcharts.model.Graph graph = new com.haulmont.charts.gui.amcharts.model.Graph();
            graph.setType(GraphType.LINE);
            graph.setDescriptionField(a.getGraphName());
            graph.setValueField(a.getDataFieldName());
            graph.setColorField(a.getGraphColor());
        }
    }

    private static class GraphTable {
        private final String graphName;
        private final String dataFieldName;
        private final String graphColor;
        private final String iteractionName;

        GraphTable(String graphName, String graphColor, String dataFieldName, String iteractionName) {
            this.graphName = graphName;
            this.graphColor = graphColor;
            this.dataFieldName = dataFieldName;
            this.iteractionName = iteractionName;
        }

        String getGraphColor() {
            return graphColor;
        }

        String getGraphName() {
            return graphName;
        }

        String getDataFieldName() {
            return dataFieldName;
        }

        String getIteractionName() {
            return iteractionName;
        }
    }

    private void setInitHasTable() {
        tableGraph.add(new GraphTable("new-contacts", "YELLOW", "new_contacts", ITRKT_NEW_CONTACT));
        tableGraph.add(new GraphTable("propose-job", "YELLOW", "propose_job", ITRKT_PROPOSE_JOB));
        tableGraph.add(new GraphTable("assign-internal-interview", "GREEN", "assign_internal_interview", ITRKT_ASSIGN_ITPEARKS_INTERVIEW));
        tableGraph.add(new GraphTable("prepare-internal-interview", "GREEN", "prepare_internal_interview", ITRKT_PREPARE_ITPEARKS_INTERVIEW));
        tableGraph.add(new GraphTable("assign-tech-interview", "GREEN", "assign_tech_interview", ITRKT_ASSIGN_TECH_INTERVIEW));
        tableGraph.add(new GraphTable("prepare-tech-interview", "GREEN", "prepare_tech_interview", ITRKT_PREPARE_TECH_INTERVIEW));
        tableGraph.add(new GraphTable("prepare-director-interview", "GREEN", "prepare_director_interview", ITRKT_PREPARE_DIRECTOR_INTERVIEW));
    }

    private void setValueDiagramData() {
        ListDataProvider dataProvider = new ListDataProvider();

        Date d = startDate;

        do {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(d);
            Date dayStart = calendar.getTime();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Date dayEnd = calendar.getTime();
            d = dayEnd;

            MapDataItem dataItem = new MapDataItem();

            for (GraphTable graphTable : tableGraph) {
                int count = countIteractionsForDay(dayStart, dayEnd, graphTable.getIteractionName());
                dataItem.add(graphTable.getDataFieldName(), count);
            }

            dataProvider.addItem(dataItem);
        } while (d.before(endDate));

        makeInterview.setDataProvider(dataProvider);
    }

    private int countIteractionsForDay(Date dayStart, Date dayEnd, String iteractionName) {
        Long count = dataManager.loadValue(QUERY_DAY_COUNT_BY_TYPE, Long.class)
                .parameter("startDate", dayStart)
                .parameter("endDate", dayEnd)
                .parameter("iteractionName", iteractionName)
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
