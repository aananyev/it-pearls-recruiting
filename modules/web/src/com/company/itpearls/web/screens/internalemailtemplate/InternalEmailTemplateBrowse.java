package com.company.itpearls.web.screens.internalemailtemplate;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.InternalEmailTemplate;

@UiController("itpearls_InternalEmailTemplate.browse")
@UiDescriptor("internal-email-template-browse.xml")
@LookupComponent("internalEmailTemplatesTable")
@LoadDataBeforeShow
public class InternalEmailTemplateBrowse extends StandardLookup<InternalEmailTemplate> {
}