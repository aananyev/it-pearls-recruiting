package com.company.itpearls.web.screens.internalemailtemplate;

import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.InternalEmailTemplate;
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
}