package com.company.itpearls.web.widgets.others;

import com.company.itpearls.core.ApplicationSetupService;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.StreamResource;
import com.haulmont.cuba.gui.components.ThemeResource;
import com.haulmont.cuba.gui.export.FileDataProvider;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("itpearls_CompanyLogoWidget")
@UiDescriptor("company-logo-widget.xml")
@DashboardWidget(name="Company Logo")
public class CompanyLogoWidget extends ScreenFragment {
    @Inject
    private Image companyLogo;
    @Inject
    private ApplicationSetupService applicationSetupService;

    @Subscribe
    public void onInit(InitEvent event) {
        FileDescriptor fileDescriptor = applicationSetupService.getActiveApplicationSetup().getApplicationLogo();

        if (fileDescriptor != null) {
            companyLogo.setSource(StreamResource.class)
                    .setStreamSupplier(() -> new FileDataProvider(fileDescriptor).provide());
        } else {
            companyLogo.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }
}