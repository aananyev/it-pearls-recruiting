package com.company.itpearls.web.screens.applicationsetup;

import com.company.itpearls.core.ApplicationSetupService;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.ApplicationSetup;

import javax.inject.Inject;

@UiController("itpearls_ApplicationSetup.edit")
@UiDescriptor("application-setup-edit.xml")
@EditedEntityContainer("applicationSetupDc")
@LoadDataBeforeShow
public class ApplicationSetupEdit extends StandardEditor<ApplicationSetup> {
    @Inject
    private ApplicationSetupService applicationSetupService;

    private Boolean flag = false;
    @Inject
    private CheckBox activeSetupField;

    @Subscribe("activeSetupField")
    public void onActiveSetupFieldValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        flag = true;
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        if (flag && activeSetupField.getValue())
            applicationSetupService.clearActiveApplicationSetup(getEditedEntity());
    }
}