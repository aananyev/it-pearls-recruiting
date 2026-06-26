package com.company.itpearls.web.widgets.reports;

import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewBuilder;
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
@DashboardWidget(name = "Воронка хантинга (рекрутер)")
public class FunnelHuntingRecrutierWidget extends ScreenFragment {

    private static final String QUERY_RESEARCHERS =
            "select e from sec$User e " +
                    "where e.active=true and e.group.name in :groupNames and " +
                    "e.name not like 'Anonymous' and e.name not like '%Test%' and e.name not like 'Administrator' " +
                    "order by e.name";

    private static final String QUERY_BULK_COUNTS =
            "select e.recrutier.id as recrutierId, it.iterationName as iteractionName, count(e) as cnt " +
                    "from itpearls_IteractionList e join e.iteractionType it " +
                    "where e.dateIteraction between :startDate and :endDate and e.recrutier in :recrutiers " +
                    "group by e.recrutier.id, it.iterationName";

    private static final View USER_DASHBOARD_VIEW = ViewBuilder.of(User.class)
            .add("name")
            .build();

    @Inject
    private UiComponents uiComponents;

    @WidgetParam
    @WindowParam
    protected Date startDate;

    @WidgetParam
    @WindowParam
    protected Date endDate;

    private List<String> listIteractionForCheck = new ArrayList<>();
    private List<User> reaearchers = new ArrayList<>();
    private Map<UUID, Map<String, Long>> bulkCounts = Collections.emptyMap();

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
        reaearchers = dataManager.load(User.class)
                .query(QUERY_RESEARCHERS)
                .view(USER_DASHBOARD_VIEW)
                .cacheable(true)
                .parameter("groupNames", Arrays.asList(HEADHUNTER, MANAGER))
                .list();
        bulkCounts = loadBulkCounts();
    }

    private Map<UUID, Map<String, Long>> loadBulkCounts() {
        if (reaearchers.isEmpty()) {
            return Collections.emptyMap();
        }

        List<KeyValueEntity> rows = dataManager.loadValues(QUERY_BULK_COUNTS)
                .properties("recrutierId", "iteractionName", "cnt")
                .parameter("startDate", startDate)
                .parameter("endDate", endDate)
                .parameter("recrutiers", reaearchers)
                .list();

        Map<UUID, Map<String, Long>> result = new HashMap<>();
        for (KeyValueEntity row : rows) {
            UUID recrutierId = row.getValue("recrutierId");
            String iteractionName = row.getValue("iteractionName");
            Number cnt = row.getValue("cnt");
            if (recrutierId != null && iteractionName != null && cnt != null) {
                result.computeIfAbsent(recrutierId, id -> new HashMap<>())
                        .put(iteractionName, cnt.longValue());
            }
        }
        return result;
    }

    private int getIteractionCount(User user, String iteractionName) {
        Map<String, Long> userCounts = bulkCounts.get(user.getId());
        if (userCounts == null) {
            return 0;
        }
        Long count = userCounts.get(iteractionName);
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
                styleCount = styleCount == 2 ? 1 : 2;

                int iteractionCount = getIteractionCount(user, a);

                Label<Integer> labelCount = uiComponents.create(Label.TYPE_INTEGER);

                labelCount.setValue(iteractionCount);
                labelCount.setWidthFull();
                labelCount.setHeightFull();
                labelCount.setStyleName("widget-mountly-interview-table-" + styleCount.toString());

                vBox.add(labelCount);
            }
        }
    }

    private void setResearcherList() {
        Integer styleCount = 2;

        for (User a : reaearchers) {
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

    private void initListIteraction() {
        listIteractionForCheck.add(ITRKT_PREPARE_ITPEARKS_INTERVIEW);
        listIteractionForCheck.add(ITRKT_PREPARE_TECH_INTERVIEW);
        listIteractionForCheck.add(ITRKT_PREPARE_DIRECTOR_INTERVIEW);
    }
}