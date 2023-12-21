package com.company.itpearls.web.main;

import com.company.itpearls.core.ApplicationSetupService;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.web.app.mainwindow.AppMainWindow;
import javax.inject.Inject;

public class ExtAppMainWindow extends AppMainWindow {
    @Inject
    private ApplicationSetupService applicationSetupService;
    @Inject
    private UiComponents uiComponents;

    @Subscribe
    private void onInit(InitEvent event) {
/*        ChangeFaviconExtension extension = new ChangeFaviconExtension();

        extension.extend(titleBar.unwrap(AbstractOrderedLayout.class),
                "./VAADIN/themes/hover/icons/no-company.png"); */

        Image image = uiComponents.create(Image.class);
        image.setSource(FileDescriptorResource.class)
                .setFileDescriptor(applicationSetupService.getActiveCompanyLogo());

        titleBar.add(image);
    }

    @Override
    protected void initLogoImage(Image logoImage) {
        logoImage.setSource(FileDescriptorResource.class)
                .setFileDescriptor(applicationSetupService.getActiveCompanyLogo());
        super.initLogoImage(logoImage);
    }
}