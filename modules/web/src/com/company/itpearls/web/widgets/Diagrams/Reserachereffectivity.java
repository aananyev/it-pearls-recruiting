package com.company.itpearls.web.widgets.Diagrams;

import com.company.itpearls.entity.IteractionList;
import com.google.common.collect.ImmutableMap;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.charts.gui.amcharts.model.Color;
import com.haulmont.charts.gui.amcharts.model.Title;
import com.haulmont.charts.gui.components.charts.SerialChart;
import com.haulmont.charts.gui.data.DataItem;
import com.haulmont.charts.gui.data.DataProvider;
import com.haulmont.charts.gui.data.ListDataProvider;
import com.haulmont.charts.gui.data.MapDataItem;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.Role;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;

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

        String usersList = "select f from sec$user f where ";

        List<User> users = dataManager.load(User.class).view("user-view").list();

        for (User a : users) {
            if(a.getGroup() != null) {
                if (a.getGroup().getName().equals(RESEARCHER)
                        || a.getGroup().getName().equals(RESEARCHER_INTERN)
                        || a.getGroup().getName().equals(RECRUTER)) {
                    int     count;
                    int     countWeek;

                    GregorianCalendar firsDayOfWeek = new GregorianCalendar();
                    firsDayOfWeek.setTime(new Date());

                    GregorianCalendar today = new GregorianCalendar();
                    today.setTime(new Date());
                    today.add(Calendar.DAY_OF_YEAR, 1);

                    if(firsDayOfWeek.get(Calendar.DAY_OF_MONTH) >= 7) {
                        firsDayOfWeek.setFirstDayOfWeek(Calendar.MONDAY);
                        firsDayOfWeek.add(Calendar.DAY_OF_YEAR, -1);
                    } else {
                        firsDayOfWeek.set(Calendar.DAY_OF_MONTH, 1);
                    }

                    if (a.getActive() & a.getFirstName() != null & a.getLastName() != null) {
                        if (iteractionName == null) {
                            count = dataManager.load(IteractionList.class)
                                    .query(queryCount)
                                    .view("iteractionList-view")
                                    .parameter("user", a)
                                    .parameter("startDate", startDate)
                                    .parameter("endDate", endDate)
                                    .list()
                                    .size();

                                countWeek = dataManager.load(IteractionList.class)
                                        .query(queryCount)
                                        .view("iteractionList-view")
                                        .parameter("user", a)
                                        .parameter("startDate", firsDayOfWeek.getTime())
                                        .parameter("endDate", today.getTime())
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

                            countWeek = dataManager.load(IteractionList.class)
                                    .query(queryCount)
                                    .view("iteractionList-view")
                                    .parameter("user", a)
                                    .parameter("iteractionName", iteractionName)
                                    .parameter("startDate", firsDayOfWeek.getTime())
                                    .parameter("endDate", today.getTime())
                                    .list()
                                    .size();
                        }

                        if (userRole != null) {
                            if (checkUserRoles(a, userRole)) {
                                dataProvider.addItem(researcherCount(a.getFirstName() + "\n" + a.getLastName(), count - countWeek, countWeek));
                            }
                        } else {
                            dataProvider.addItem(researcherCount(a.getFirstName() + "\n" + a.getLastName(), count - countWeek, countWeek));
                        }
                    }
                }
            }
        }

        return dataProvider;
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

    public Boolean checkUserRoles(User user, String role) {
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