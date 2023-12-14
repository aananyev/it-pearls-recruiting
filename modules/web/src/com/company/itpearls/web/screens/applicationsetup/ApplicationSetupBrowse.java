package com.company.itpearls.web.screens.applicationsetup;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.ApplicationSetup;

@UiController("itpearls_ApplicationSetup.browse")
@UiDescriptor("application-setup-browse.xml")
@LookupComponent("applicationSetupsTable")
@LoadDataBeforeShow
public class ApplicationSetupBrowse extends StandardLookup<ApplicationSetup> {
}