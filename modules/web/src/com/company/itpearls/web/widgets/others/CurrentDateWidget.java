package com.company.itpearls.web.widgets.others;

import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

@UiController("itpearls_CurrentDateWidget")
@UiDescriptor("current-date-widget.xml")
@DashboardWidget(name = "Current Date")
public class CurrentDateWidget extends ScreenFragment {
    @Inject
    private Label<String> currentTimeLabel;
    @Inject
    private Label<String> currentDateLabel;
    @Inject
    private Label<String> currentDayOfWeekLabel;

    @Subscribe
    public void onInit(InitEvent event) {
        setCurrentTime();
        setCurrentDate();
        setCurrentDayOfWeek();
    }

    private void setCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("H:m");
        currentTimeLabel.setValue(sdf.format(new Date()));
    }

    private void setCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy");
        currentDateLabel.setValue(sdf.format(new Date()));
    }

    private void setCurrentDayOfWeek() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
        currentDayOfWeekLabel.setValue(sdf.format(new Date()));
    }
}