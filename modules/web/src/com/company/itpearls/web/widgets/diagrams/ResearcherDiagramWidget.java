package com.company.itpearls.web.widgets.diagrams;

import com.company.itpearls.entity.IteractionList;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.charts.gui.amcharts.model.GraphType;
import com.haulmont.charts.gui.components.charts.SerialChart;
import com.haulmont.charts.gui.data.ListDataProvider;
import com.haulmont.charts.gui.data.MapDataItem;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import org.graalvm.compiler.graph.Graph;

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
    private String ITRKT_NEW_CONTACT = "Новый контакт";
    private String ITRKT_PROPOSE_JOB = "Предложение работы";
    private String ITRKT_ASSIGN_ITPEARKS_INTERVIEW = "Назначено собеседование с рекрутером IT Pearls";
    private String ITRKT_PREPARE_ITPEARKS_INTERVIEW = "Прошел собеседование с рекрутером IT Pearls";
    private String ITRKT_ASSIGN_TECH_INTERVIEW = "Назначено техническое собеседование";
    private String ITRKT_PREPARE_TECH_INTERVIEW = "Прошел техническое собеседование";
    private String ITRKT_PREPARE_DIRECTOR_INTERVIEW = "Прошел собеседование с Директором";

    List<GraphTable>    tableGraph = new ArrayList<>();

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
        for(GraphTable a : tableGraph ) {
            com.haulmont.charts.gui.amcharts.model.Graph graph = new com.haulmont.charts.gui.amcharts.model.Graph();
            graph.setType(GraphType.LINE);
            graph.setDescriptionField(a.getGraphName());
            graph.setValueField(a.getDataFieldName());
            graph.setColorField(a.getGraphColor());
        }

    }

    class GraphTable {
        String  graphName;
        String  dataFieldName;
        String  graphColor;
        String  query;

        GraphTable(String graphName, String graphColor, String dataFieldName, String query) {
            put(graphName, graphColor, dataFieldName, query);
        }

        public void put(String graphName, String graphColor, String dataFieldName, String query) {
            setGraphName(graphName);
            setDataFieldNamw(dataFieldName);
            setQuery(query);
            setGraphColor(graphColor);

        }

        public String getGraphColor() {
            return graphColor;
        }

        public void setGraphColor(String graphColor) {
            this.graphColor = graphColor;
        }

        public String getGraphName() {
            return graphName;
        }

        public String getDataFieldName() {
            return dataFieldName;
        }

        public String getQuery() {
            return query;
        }

        public void setDataFieldNamw(String dataFieldName) {
            this.dataFieldName = dataFieldName;
        }

        public void setGraphName(String graphName) {
            this.graphName = graphName;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }

    private void setInitHasTable() {
       tableGraph.add(new GraphTable("new-contacts","YELLOW", "new_contacts", ITRKT_NEW_CONTACT));
       tableGraph.add(new GraphTable("propose-job", "YELLOW", "propose_job", ITRKT_PROPOSE_JOB));
       tableGraph.add(new GraphTable("assign-internal-interview", "GREEN", "assign_internal_interview", ITRKT_ASSIGN_ITPEARKS_INTERVIEW));
       tableGraph.add(new GraphTable("prepare-internal-interview", "GREEN", "prepare_internal_interview", ITRKT_PREPARE_ITPEARKS_INTERVIEW));
       tableGraph.add(new GraphTable("assign-tech-interview", "GREEN", "assign_tech_interview", ITRKT_ASSIGN_TECH_INTERVIEW));
       tableGraph.add(new GraphTable("prepare-tech-interview", "GREEN", "prepare_tech_interview", ITRKT_PREPARE_TECH_INTERVIEW));
       tableGraph.add(new GraphTable("prepare-director-interview", "GREEN", "prepare_director_interview", ITRKT_PREPARE_DIRECTOR_INTERVIEW));
    }

    private void setValueDiagramData() {
        String QUERY_FOR_DIAGRAMM = "select f " +
                "from itpearls_IteractionList f " +
                "where f.dateIteraction between :startDate and :endDate " +
                "order by f.dateIteraction";
        String QUERY_FOR_DIAGRAMM_NEW_CONTACTS = "select f " +
                "from itpearls_IteractionList f " +
                "where (f.dateIteraction between :startDate and :endDate) and " +
                "f.iteractionType = (select e from itpearls_Iteraction e where e.iterationName like  \'" +
                ITRKT_NEW_CONTACT+ "\') "+
                "order by f.dateIteraction";
        String QUERY_FOR_DIAGRAMM_ITERACTION_TYPE = "select f " +
                "from itpearls_IteractionList f " +
                "where (f.dateIteraction between :startDate and :endDate) and " +
                "f.iteractionType = (select e from itpearls_Iteraction e where e.iterationName like  \'" +
                ITRKT_ASSIGN_ITPEARKS_INTERVIEW + "\') "+
                "order by f.dateIteraction";

        ListDataProvider dataProvider = new ListDataProvider();

        Date d = startDate;

        List<Graph> graphs = new ArrayList<>();

        do {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(d);
            calendar.add(Calendar.DAY_OF_MONTH,1);

            Date endDay = calendar.getTime();
            d = calendar.getTime();

            MapDataItem dataItem = new MapDataItem();

            for(GraphTable a: tableGraph ) {
                List<IteractionList> iteraction = new ArrayList<>();

                LoadContext<IteractionList> loadCont  = LoadContext.create(IteractionList.class)
                        .setQuery(LoadContext.createQuery(a.getQuery())
                                .setParameter("startDate", d)
                                .setParameter("endDate", endDay))
                        .setView("iteractionList-view");
                iteraction = dataManager.loadList(loadCont);

                Integer count = iteraction.size();

                dataItem.add(a.dataFieldName, count);
            }

            dataProvider.addItem(dataItem);
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