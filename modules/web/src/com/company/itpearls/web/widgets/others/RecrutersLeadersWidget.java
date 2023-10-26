package com.company.itpearls.web.widgets.others;

import com.company.itpearls.core.RecruterStatService;
import com.company.itpearls.entity.ExtUser;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.core.global.DataManager;
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
@DashboardWidget(name = "Recruters Leaders")
public class RecrutersLeadersWidget extends ScreenFragment {
    List<ExtUser> recruters = new ArrayList<>();
    Map<ExtUser, Integer> grade = new HashedMap();
    Map<ExtUser, Integer> interviews = new HashedMap();
    static final String QUERY_GET_RECRUTERS_LIST = "select e from itpearls_ExtUser e " +
            "where e.active = true and e.dashboards = true order by e.name";

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
                        (a,b) -> {throw new AssertionError(); },
                        LinkedHashMap::new
                ));

        for (Map.Entry<ExtUser, Integer> empl : gradeSorted.entrySet()) {
            Label label = uiComponents.create(Label.class);
            label.setWidthFull();
            label.setHeightAuto();

            label.setValue(empl.getKey().getName()
                    + " "
                    + recruterStatService.getGradeName(empl.getValue())
                    + " / "
                    + interviews.get(empl.getKey()))
            ;

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
                .view("extUser-view")
                .list();

        for (ExtUser emloyee : employees) {
            if (emloyee.getUserRoles().size() > 0) {
                recruters.add(emloyee);

                int interactions = recruterStatService.countInteraction(emloyee);
                interviews.put(emloyee, interactions);
                grade.put(emloyee, recruterStatService.getGrade(interactions));
            }
        }
    }
}