package com.company.itpearls.web.widgets;

import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("itpearls_ResearcherResultWidget")
@UiDescriptor("researcher-result-widget.xml")
@DashboardWidget(name = "Researcher KPI")
public class ResearcherResultWidget extends ScreenFragment {
    @Inject
    private Label<String> title;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        title.setValue( "<b><u>Ресерчеры</u></b>" );
    }
}