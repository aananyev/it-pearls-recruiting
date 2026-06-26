package com.company.itpearls.web.widgets.others;

import com.company.itpearls.core.RecruterStatService;
import com.company.itpearls.entity.ExtUser;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.ScrollBoxLayout;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import org.apache.commons.collections.map.HashedMap;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@UiController("itpearls_RecrutersLeadersWidget")
@UiDescriptor("recruters-leaders-widget.xml")
@DashboardWidget(name = "Лидеры среди рекрутеров")
public class RecrutersLeadersWidget extends ScreenFragment {

    private static final String QUERY_GET_RECRUTERS_LIST =
            "select e from itpearls_ExtUser e " +
                    "where e.active = true and e.dashboards = true " +
                    "and exists (select 1 from sec$UserRole ur where ur.user = e) " +
                    "order by e.name";

    private static final String QUERY_BULK_INTERVIEW_COUNTS =
            "select e.recrutier.id as recrutierId, count(e) as cnt from itpearls_IteractionList e " +
                    "where e.recrutier in :recrutiers and " +
                    "@between(e.dateIteraction, now-30, now+1, day) and " +
                    "(e.iteractionType.signOurInterviewAssigned = true or e.iteractionType.signOurInterview = true) " +
                    "group by e.recrutier.id";

    private static final View EXT_USER_DASHBOARD_VIEW = ViewBuilder.of(ExtUser.class)
            .add("name")
            .build();

    List<ExtUser> recruters = new ArrayList<>();
    Map<ExtUser, Integer> grade = new HashedMap();
    Map<ExtUser, Integer> interviews = new HashedMap();

    @Inject
    private DataManager dataManager;
    @Inject
    private RecruterStatService recruterStatService;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScrollBoxLayout leadersTableWidget;

    @Subscribe
    public void onInit(InitEvent event) {
        getRecrutersList();
        calculateLeader();
        setLabels();
    }

    private ExtUser calculateLeader() {
        ExtUser bestUser = interviews.entrySet().iterator().next().getKey();

        for (Map.Entry<ExtUser, Integer> entry : interviews.entrySet()) {
            for (Map.Entry<ExtUser, Integer> entry1 : interviews.entrySet()) {
                if (entry1.getValue() > entry.getValue()) {
                    bestUser = entry1.getKey();
                }
            }
        }

        return null;
    }

    private void setLabels() {
        boolean even = false;
        ExtUser bestUser = calculateLeader();

        Map<ExtUser, Integer> gradeSorted = grade.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new
                ));

        for (Map.Entry<ExtUser, Integer> empl : gradeSorted.entrySet()) {
            Label label = uiComponents.create(Label.class);
            label.setWidthFull();
            label.setHeightAuto();

            StringBuilder sb = new StringBuilder();
            sb.append(empl.getKey().getName())
                    .append(" ")
                    .append(recruterStatService.getGradeName(empl.getValue()))
                    .append(" / ")
                    .append(interviews.get(empl.getKey()));
            label.setValue(sb.toString());

            leadersTableWidget.add(label);

            if (even) {
                if (empl.equals(bestUser)) {
                    label.setStyleName("widget-leaders-table-row-background-gray");
                } else {
                    label.setStyleName("widget-table-row-background-gray");
                }
            } else {
                if (empl.equals(bestUser)) {
                    label.setStyleName("widget-leaders-table-row-background-white");
                } else {
                    label.setStyleName("widget-table-row-background-white");
                }
            }

            even = !even;
        }
    }

    private void getRecrutersList() {
        List<ExtUser> employees = dataManager.load(ExtUser.class)
                .query(QUERY_GET_RECRUTERS_LIST)
                .view(EXT_USER_DASHBOARD_VIEW)
                .cacheable(true)
                .list();

        Map<UUID, Long> interviewCounts = loadInterviewCounts(employees);

        for (ExtUser employee : employees) {
            recruters.add(employee);

            int interactionCount = interviewCounts.getOrDefault(employee.getId(), 0L).intValue();
            interviews.put(employee, interactionCount);
            grade.put(employee, recruterStatService.getGrade(interactionCount));
        }
    }

    private Map<UUID, Long> loadInterviewCounts(List<ExtUser> employees) {
        if (employees.isEmpty()) {
            return Collections.emptyMap();
        }

        List<KeyValueEntity> rows = dataManager.loadValues(QUERY_BULK_INTERVIEW_COUNTS)
                .properties("recrutierId", "cnt")
                .parameter("recrutiers", employees)
                .list();

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
}
