package com.company.itpearls.web.screens.internalemailer;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.InternalEmailer;

@UiController("itpearls_Emailer.browse")
@UiDescriptor("internal-emailer-browse.xml")
@LookupComponent("emailersTable")
@LoadDataBeforeShow
public class InternalEmailerBrowse extends StandardLookup<InternalEmailer> {
}