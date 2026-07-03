package com.company.hunttech.web.widgets.others;

import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

@UiController("hunttech_HunttechLogoWidget")
@UiDescriptor("hunttech-logo-widget.xml")
@DashboardWidget(name="HuntTech Logo")
public class HunttechLogoWidget extends ScreenFragment {
}
