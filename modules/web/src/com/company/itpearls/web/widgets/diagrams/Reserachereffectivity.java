package com.company.itpearls.web.widgets.diagrams;

import com.company.itpearls.entity.ExtUser;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.charts.gui.amcharts.model.Color;
import com.haulmont.charts.gui.amcharts.model.Title;
import com.haulmont.charts.gui.components.charts.SerialChart;
import com.haulmont.charts.gui.data.DataItem;
import com.haulmont.charts.gui.data.DataProvider;
import com.haulmont.charts.gui.data.ListDataProvider;
import com.haulmont.charts.gui.data.MapDataItem;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.Role;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;

@UiController("itpearls_Reserachereffectivity")
@UiDescriptor("ReseracherEffectivity.xml")
@DashboardWidget(name = "Эффективность ресерчера")
public class Reserachereffectivity extends ScreenFragment {

    private static final String QUERY_INTERACTION_COUNTS_BY_RECRUTIER =
            "select e.recrutier.id as recrutierId, count(e) as cnt from itpearls_IteractionList e " +
                    "where e.recrutier is not null " +
                    "and e.dateIteraction between :startDate and :endDate ";

    private static final String QUERY_INTERACTION_COUNTS_BY_RECRUTIER_WITH_TYPE =
            "select e.recrutier.id as recrutierId, count(e) as cnt from itpearls_IteractionList e " +
                    "where e.recrutier is not null " +
                    "and e.dateIteraction between :startDate and :endDate " +
                    "and e.iteractionType = (select i from itpearls_Iteraction i where i.iterationName like :iteractionName) ";

    private static final String QUERY_RESEARCHERS =
            "select f from itpearls_ExtUser f where f.active = true " +
                    "and f.firstName is not null and f.lastName is not null " +
                    "and f.group.name in :groupNames";

    private static final String QUERY_RESEARCHERS_WITH_ROLE =
            "select f from itpearls_ExtUser f where f.active = true " +
                    "and f.firstName is not null and f.lastName is not null " +
                    "and f.group.name in :groupNames " +
                    "and exists (select 1 from sec$UserRole ur join ur.role r where ur.user = f and r.name like :userRole)";

    private static final View RESEARCHER_DASHBOARD_VIEW = ViewBuilder.of(ExtUser.class)
            .add("firstName")
            .add("lastName")
            .build();

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
    protected String userGroup;

    @WidgetParam
    @WindowParam
    protected String userRole;

    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private static String GRAPH_X = "recrutier";
    private static String GRAPH_Y = "count";
    private static String GRAPH_Y_WEEK = "countLastWeek";
    private String RESEARCHER = "Ресерчинг";
    private String RESEARCHER_INTERN = "Стажер";
    private String RECRUTER = "Хантинг";

    @Inject
    private SerialChart countIteractionChart;
    @Inject
    private DataManager dataManager;

    @Subscribe
    public void onInit(InitEvent event) {
        setDeafaultTimeInterval();

        countIteractionChart.setCategoryField(GRAPH_X);
        countIteractionChart.setDataProvider(valueGraphs());

        setUserGroup();

        setDiagramTitle();
    }

    private void setUserGroup() {
        if(userGroup == null) {
            userGroup = RESEARCHER;
        }
    }

    private DataProvider valueGraphs() {
        ListDataProvider dataProvider = new ListDataProvider();

        Date[] weekInterval = getWeekInterval();
        Map<UUID, Long> fullIntervalCounts = loadInteractionCountsByRecrutier(startDate, endDate);
        Map<UUID, Long> weekIntervalCounts = loadInteractionCountsByRecrutier(weekInterval[0], weekInterval[1]);

        List<ExtUser> users = loadResearchers();

        for (ExtUser a : users) {
            int count = fullIntervalCounts.getOrDefault(a.getId(), 0L).intValue();
            int countWeek = weekIntervalCounts.getOrDefault(a.getId(), 0L).intValue();
            dataProvider.addItem(researcherCount(
                    a.getFirstName() + "\n" + a.getLastName(),
                    count - countWeek,
                    countWeek));
        }

        return dataProvider;
    }

    private List<ExtUser> loadResearchers() {
        List<String> groupNames = Arrays.asList(RESEARCHER, RESEARCHER_INTERN, RECRUTER);
        if (userRole != null) {
            return dataManager.load(ExtUser.class)
                    .query(QUERY_RESEARCHERS_WITH_ROLE + " order by f.lastName, f.firstName")
                    .view(RESEARCHER_DASHBOARD_VIEW)
                    .cacheable(true)
                    .parameter("groupNames", groupNames)
                    .parameter("userRole", userRole)
                    .list();
        }
        return dataManager.load(ExtUser.class)
                .query(QUERY_RESEARCHERS + " order by f.lastName, f.firstName")
                .view(RESEARCHER_DASHBOARD_VIEW)
                .cacheable(true)
                .parameter("groupNames", groupNames)
                .list();
    }

    private Map<UUID, Long> loadInteractionCountsByRecrutier(Date from, Date to) {
        String query = iteractionName == null
                ? QUERY_INTERACTION_COUNTS_BY_RECRUTIER + "group by e.recrutier.id"
                : QUERY_INTERACTION_COUNTS_BY_RECRUTIER_WITH_TYPE + "group by e.recrutier.id";

        List<KeyValueEntity> rows;
        if (iteractionName == null) {
            rows = dataManager.loadValues(query)
                    .properties("recrutierId", "cnt")
                    .parameter("startDate", from)
                    .parameter("endDate", to)
                    .list();
        } else {
            rows = dataManager.loadValues(query)
                    .properties("recrutierId", "cnt")
                    .parameter("startDate", from)
                    .parameter("endDate", to)
                    .parameter("iteractionName", iteractionName)
                    .list();
        }

        Map<UUID, Long> result = new HashMap<>();
        for (KeyValueEntity row : rows) {
            UUID recrutierId = row.getValue("recrutierId");
            Number cnt = row.getValue("cnt");
            if (recrutierId != null && cnt != null) {
                result.put(recrutierId, cnt.longValue());
            }
        }
        return result;
    }

    private Date[] getWeekInterval() {
        GregorianCalendar firstDayOfWeek = new GregorianCalendar();
        firstDayOfWeek.setTime(new Date());

        GregorianCalendar today = new GregorianCalendar();
        today.setTime(new Date());
        today.add(Calendar.DAY_OF_YEAR, 1);

        if (firstDayOfWeek.get(Calendar.DAY_OF_MONTH) >= 7) {
            firstDayOfWeek.setFirstDayOfWeek(Calendar.MONDAY);
            firstDayOfWeek.add(Calendar.DAY_OF_YEAR, -1);
        } else {
            firstDayOfWeek.set(Calendar.DAY_OF_MONTH, 1);
        }

        return new Date[]{firstDayOfWeek.getTime(), today.getTime()};
    }

    private DataItem researcherCount(String s, int i, int countWeek) {
        MapDataItem item = new MapDataItem();

        item.add(GRAPH_X, s);
        item.add(GRAPH_Y_WEEK, i);
        item.add(GRAPH_Y, countWeek);

        return item;
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
                .setText("Взаимодействия" +
                        (iteractionName != null ? ": " : "") + iteractionName)
                .setAlpha(1.0)
                .setColor(Color.BLACK));
        titles.add(new Title().setText(dateFormat.format(startDate) + " - " +
                dateFormat.format(endDate)).setAlpha(1.0).setColor(Color.BROWN).setSize(12));

        countIteractionChart.setTitles(titles);
    }

    public Boolean checkUserRoles(ExtUser user, String role) {
        Role s = dataManager.load(Role.class)
                .query("select e from sec$Role e where e.name like :roleName")
                .parameter("roleName", role)
                .one();

        if(role != null) {
            if( user.getUserRoles() != null )
                return user.getUserRoles().contains(s);
            else
                return false;
        } else
            return false;
    }
}
