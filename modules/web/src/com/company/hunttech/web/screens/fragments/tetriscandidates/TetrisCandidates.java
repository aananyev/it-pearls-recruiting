package com.company.hunttech.web.screens.fragments.tetriscandidates;

import com.haulmont.addon.dashboard.gui.components.DashboardFrame;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.Collections;

@UiController("hunttech_TetrisCandidates")
@UiDescriptor("tetris-candidates.xml")
public class TetrisCandidates extends Screen {

    public static final String DASHBOARD_CODE = "tetris-candidates-dashboard";
    public static final String DASHBOARD_JSON = "com/company/hunttech/web/screens/mainscreen/dashboards/tetris-candidates-dashboard.json";

    @Inject
    protected DashboardFrame tetrisCandidatesDashboard;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        initTetrisDashboard();
    }

    protected void initTetrisDashboard() {
        if (tetrisCandidatesDashboard == null) {
            return;
        }
        tetrisCandidatesDashboard.setCode(DASHBOARD_CODE);
        tetrisCandidatesDashboard.setJsonPath(DASHBOARD_JSON);
        tetrisCandidatesDashboard.init(Collections.emptyMap());
    }
}