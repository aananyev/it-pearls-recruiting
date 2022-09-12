package com.company.itpearls.web.screens.emailer;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Emailer;

@UiController("itpearls_Emailer.browse")
@UiDescriptor("emailer-browse.xml")
@LookupComponent("emailersTable")
@LoadDataBeforeShow
public class EmailerBrowse extends StandardLookup<Emailer> {
}