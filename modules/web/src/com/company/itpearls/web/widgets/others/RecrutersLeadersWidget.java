package com.company.itpearls.web.widgets.others;

import com.company.itpearls.core.RecruterStatService;
import com.company.itpearls.entity.ExtUser;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.ScrollBoxLayout;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import org.apache.commons.collections.map.HashedMap;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@UiController("itpearls_RecrutersLeadersWidget")
@UiDescriptor("recruters-leaders-widget.xml")
@DashboardWidget(name = "Recruters Leaders")
public class RecrutersLeadersWidget extends ScreenFragment {
    List<ExtUser> recruters = new ArrayList<>();
    Map<ExtUser, Integer> grade = new HashedMap();
    Map<ExtUser, Integer> interviews = new HashedMap();

    @Inject
    private DataManager dataManager;
    static final String QUERY_GET_RECRUTERS_LIST = "select e from itpearls_ExtUser e " +
            "where e.active = true and e.dashboards = true order by e.name";
    @Inject
    private RecruterStatService recruterStatService;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScrollBoxLayout leadersTableWidget;

    @Subscribe
    public void onInit(InitEvent event) {
        getRecrutersList();
        setLabels();
    }

    private void setLabels() {
        boolean even = false;
        for (ExtUser empl : recruters) {
            Label label = uiComponents.create(Label.class);
            label.setWidthFull();
            label.setHeightAuto();

            label.setValue(empl.getName()
                    + " "
                    + recruterStatService.getGradeName(grade.get(empl))
                    + " / "
                    + interviews.get(empl));

            leadersTableWidget.add(label);
            if (even) {
                label.setStyleName("widget-table-row-background-gray");
            } else {
                label.setStyleName("widget-table-row-background-white");
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