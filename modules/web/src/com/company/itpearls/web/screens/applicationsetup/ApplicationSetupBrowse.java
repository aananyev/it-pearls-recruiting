package com.company.itpearls.web.screens.applicationsetup;

import com.company.itpearls.core.ApplicationSetupService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.export.FileDataProvider;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.ApplicationSetup;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.web.App;

import javax.inject.Inject;

@UiController("itpearls_ApplicationSetup.browse")
@UiDescriptor("application-setup-browse.xml")
@LookupComponent("applicationSetupsTable")
@LoadDataBeforeShow
public class ApplicationSetupBrowse extends StandardLookup<ApplicationSetup> {
    @Inject
    private CollectionLoader<ApplicationSetup> applicationSetupsDl;

    private Boolean flag = false;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private GroupTable<ApplicationSetup> applicationSetupsTable;
    @Inject
    private Filter filter;

    ApplicationSetup applicationSetup = null;
    @Inject
    private UiComponents uiComponents;

    @Subscribe(id = "applicationSetupsDc", target = Target.DATA_CONTAINER)
    public void onApplicationSetupsDcItemChange(InstanceContainer.ItemChangeEvent<ApplicationSetup> event) {
        if (flag) {
            flag = false;
            applicationSetupsDl.load();
            applicationSetupsTable.repaint();
        }
    }

    public void editActionHandler() {
        applicationSetup = applicationSetupsTable.getSingleSelected();
        screenBuilders.editor(applicationSetupsTable)
                .build()
                .show();

        applicationSetupsTable.scrollTo(applicationSetup);
        applicationSetupsTable.setSelected(applicationSetup);
        applicationSetupsDl.load();

        flag = true;
    }

    public void createActionHandler() {
        screenBuilders.editor(applicationSetupsTable)
                .newEntity()
                .build()
                .show();
        if (applicationSetup != null) {
            applicationSetupsTable.scrollTo(applicationSetup);
            applicationSetupsTable.setSelected(applicationSetup);
        }

        flag = true;
    }

    private Component retColumnGeneratorImage(FileDescriptor fileDescriptor) {
        HBoxLayout hBoxLayout = uiComponents.create(HBoxLayout.class);
        hBoxLayout.setWidthFull();
        hBoxLayout.setHeightFull();

        Image image = uiComponents.create(Image.class);
        image.setWidth("50px");
        image.setHeight("50px");
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (fileDescriptor != null) {
            image.setSource(StreamResource.class)
                    .setStreamSupplier(() -> new FileDataProvider(fileDescriptor).provide());
        } else {
            image.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }

        hBoxLayout.add(image);
        return hBoxLayout;

    }
    public Component applicationLogoGenerator(ApplicationSetup entity) {
        return retColumnGeneratorImage(entity.getApplicationLogo());
    }

    public Component applicationIconGenerator(ApplicationSetup entity) {
        return retColumnGeneratorImage(entity.getApplicationIcon());
    }
}