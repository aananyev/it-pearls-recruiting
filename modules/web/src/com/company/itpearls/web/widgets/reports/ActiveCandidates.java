package com.company.itpearls.web.widgets.reports;

import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("itpearls_ActiveCandidates")
@UiDescriptor("active-candidates.xml")
@DashboardWidget(name = "Active Candidates Widget")
public class ActiveCandidates extends ScreenFragment {
    @Inject
    private Label<String> widgetTitle;

    private String widgetTitleString = "Список активных кандидатов";

    @Subscribe
    public void onInit(InitEvent event) {
       setTitle();

    }

    private void setTitle() {
        widgetTitle.setValue(widgetTitleString);
    }

}