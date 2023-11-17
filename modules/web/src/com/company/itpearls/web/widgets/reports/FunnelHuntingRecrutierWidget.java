package com.company.itpearls.web.widgets.reports;

import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@UiController("itpearls_FunnelHuntingRecrutierWidget")
@UiDescriptor("funnel-hunting-recrutier-widget.xml")
@DashboardWidget(name = "Funnel Hunting Recruter Widget")
public class FunnelHuntingRecrutierWidget extends ScreenFragment {
    @Inject
    private UiComponents uiComponents;

    @WidgetParam
    @WindowParam
    protected Date startDate;

    @WidgetParam
    @WindowParam
    protected Date endDate;

    private List<String> listIteractionForCheck = new ArrayList<String>();
    private List<User> reaearchers = new ArrayList<>();

    private static final String ITRKT_NEW_CONTACT = "Новый контакт";
    private static final String ITRKT_POPOSE_JOB = "Предложение работы";
    private static final String ITRKT_ASSIGN_ITPEARKS_INTERVIEW = "Назначено собеседование с рекрутером IT Pearls";
    private static final String ITRKT_PREPARE_ITPEARKS_INTERVIEW = "Прошел собеседование с рекрутером IT Pearls";
    private static final String ITRKT_ASSIGN_TECH_INTERVIEW = "Назначено техническое собеседование";
    private static final String ITRKT_PREPARE_TECH_INTERVIEW = "Прошел техническое собеседование";
    private static final String ITRKT_PREPARE_DIRECTOR_INTERVIEW = "Прошел собеседование с Директором";

    private static final String labelHeight = "15px";
    private static final String sizeColumn = "115px";
    private static final String HEADHUNTER = "Хантинг";
    private static final String MANAGER = "Менеджмент";

    @Inject
    private HBoxLayout boxWidgetTitle;
    @Inject
    private DataManager dataManager;
    @Inject
    private VBoxLayout researcherNameBox;
    @Inject
    private Label<String> widgetTitle;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        initListIteraction();

        setDeafaultTimeInterval();
        setWidgetTitle();
        getResearchersList();

        setIteractionTitle();
        setResearcherList();
    }

    private void setWidgetTitle() {
        String title = "Статистика по рекрутерам за: ";
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append(title)
                .append(df.format(startDate))
                .append(" - ")
                .append(df.format(endDate));

//        widgetTitle.setValue(title + df.format(startDate) + " - " + df.format(endDate));
        widgetTitle.setValue(sb.toString());
    }

    private void getResearchersList() {
        LoadContext<User> loadContext = LoadContext.create(User.class)
                .setQuery(LoadContext.createQuery("select e from sec$User e " +
                        "where e.active=true and " +
                        "e.name not like \'Anonymous\' and " +
                        "e.name not like \'%Test%\' and " +
                        "e.name not like \'Administrator\' " +
                        "order by e.name"))
                .setView("user-view");
        reaearchers = dataManager.loadList(loadContext);
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

    private void setIteractionTitle() {

        for (String a : listIteractionForCheck) {
            // бокс вертикальный для набора статистики
            VBoxLayout vBox = uiComponents.create(VBoxLayout.class);
            vBox.setAlignment(Component.Alignment.BOTTOM_CENTER);
            vBox.setSpacing(false);
            vBox.setWidth(sizeColumn);

            boxWidgetTitle.add(vBox);
            // заголовок
            Label<String> label = uiComponents.create(Label.TYPE_STRING);
            label.setWidthFull();
            label.setHeightFull();
            label.setStyleName("widget-table-header");
            label.setValue(a);

            vBox.add(label);
            vBox.expand(label);

            Integer styleCount = 2;

            for (User user : reaearchers) {

                if (user.getGroup() != null) {
                    if (user.getGroup().getName().equals(HEADHUNTER) || user.getGroup().getName().equals(MANAGER)) {
                        styleCount = styleCount == 2 ? 1 : 2;

                        String queryCounter = "select count(e) from itpearls_IteractionList e " +
                                "where e.dateIteraction between :startDate and :endDate and " +
                                "e.recrutier = :recrutier and " +
                                "e.iteractionType = (select f from itpearls_Iteraction f where f.iterationName like :iteractionName )";

                        int iteractionCount = dataManager.loadValue(queryCounter, Integer.class)
                                .parameter("startDate", startDate)
                                .parameter("endDate", endDate)
                                .parameter("recrutier", user)
                                .parameter("iteractionName", a)
                                .one();


                        Label<Integer> labelCount = uiComponents.create(Label.TYPE_INTEGER);

                        labelCount.setValue(iteractionCount);
                        labelCount.setWidthFull();
                        labelCount.setHeightFull();
                        labelCount.setStyleName("widget-mountly-interview-table-" + styleCount.toString());

                        vBox.add(labelCount);
                    }
                }
            }
        }
    }

    private void setResearcherList() {
        Integer styleCount = 2;

        for (User a : reaearchers) {

            if (a.getGroup() != null) {
                if (a.getGroup().getName().equals(HEADHUNTER) || a.getGroup().getName().equals(MANAGER)) {
                    styleCount = styleCount == 2 ? 1 : 2;

                    CssLayout boxLayout = uiComponents.create(CssLayout.class);
                    boxLayout.setWidthFull();
                    boxLayout.setAlignment(Component.Alignment.BOTTOM_LEFT);
                    boxLayout.setStyleName("widget-mountly-interview-table-" + styleCount.toString());

                    Label<String> label = uiComponents.create(Label.TYPE_STRING);
                    label.setAlignment(Component.Alignment.MIDDLE_LEFT);
                    label.setWidthFull();
                    label.setStyleName("widget-mountly-interview-table-" + styleCount.toString());
                    label.setValue(a.getName());

                    boxLayout.add(label);
                    researcherNameBox.add(boxLayout);
                }
            }
        }
    }

    private void initListIteraction() {
        listIteractionForCheck.add(ITRKT_PREPARE_ITPEARKS_INTERVIEW);
        listIteractionForCheck.add(ITRKT_PREPARE_TECH_INTERVIEW);
        listIteractionForCheck.add(ITRKT_PREPARE_DIRECTOR_INTERVIEW);
    }
}