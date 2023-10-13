package com.company.itpearls.web.widgets.others;

import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;

@UiController("itpearls_MyNameWidget")
@UiDescriptor("my-name-widget.xml")
@DashboardWidget(name="My Name")
public class MyNameWidget extends ScreenFragment {

    @Inject
    private Label<String> myNameLabel;
    @Inject
    private UserSession userSession;
    @Inject
    private Label<String> myGroupLabel;

    @Subscribe
    public void onInit(InitEvent event) {
        setUserGroup();
        setRecruterName();
    }

    private void setRecruterName() {
        myNameLabel.setValue(userSession.getUser().getName());
    }

    private void setUserGroup() {
        myGroupLabel.setValue(userSession.getUser().getGroup().getName());
    }
}