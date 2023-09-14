package com.company.itpearls.web.screens.internalemailtemplate;

import com.company.itpearls.entity.InternalEmailerTemplate;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.InternalEmailTemplate;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;

@UiController("itpearls_InternalEmailTemplate.browse")
@UiDescriptor("internal-email-template-browse.xml")
@LookupComponent("internalEmailTemplatesTable")
@LoadDataBeforeShow
public class InternalEmailTemplateBrowse extends StandardLookup<InternalEmailTemplate> {
    @Inject
    private CollectionLoader<InternalEmailTemplate> internalEmailTemplatesDl;
    @Inject
    private UserSession userSession;
    @Inject
    private GroupTable<InternalEmailTemplate> internalEmailTemplatesTable;
    @Inject
    private UiComponents uiComponents;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        loadDefaultFilter();
    }

    private void loadDefaultFilter() {
        internalEmailTemplatesDl.removeParameter("shareTemplate");
        internalEmailTemplatesDl.setParameter("templateAuthor", userSession.getUser());
        internalEmailTemplatesDl.load();
    }

    @Subscribe("viewOtherCheckBox")
    public void onViewOtherCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            internalEmailTemplatesDl.setParameter("shareTemplate", true);
            internalEmailTemplatesDl.removeParameter("templateAuthor");
            internalEmailTemplatesTable.setEditable(false);
            internalEmailTemplatesDl.load();
        } else {
            loadDefaultFilter();
            internalEmailTemplatesTable.setEditable(true);
        }
    }

    public Component projectFileImageGenegator(InternalEmailTemplate entity) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        Image image = uiComponents.create(Image.class);
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);
        image.setWidth("50px");
        image.setHeight("50px");
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);

        if (entity.getTemplateOpenPosition() != null) {
            image.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(entity
                    .getTemplateOpenPosition()
                    .getProjectName()
                    .getProjectLogo());
        } else {
            image.setSource(ThemeResource.class)
                    .setPath("icons/no-company.png");
        }

        retHBox.add(image);

        return retHBox;
    }
}