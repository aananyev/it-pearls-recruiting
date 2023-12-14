package com.company.itpearls.web.screens.applicationsetup;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.ApplicationSetup;

@UiController("itpearls_ApplicationSetup.edit")
@UiDescriptor("application-setup-edit.xml")
@EditedEntityContainer("applicationSetupDc")
@LoadDataBeforeShow
public class ApplicationSetupEdit extends StandardEditor<ApplicationSetup> {
}