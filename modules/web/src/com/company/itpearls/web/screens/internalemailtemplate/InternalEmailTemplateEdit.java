package com.company.itpearls.web.screens.internalemailtemplate;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.InternalEmailTemplate;

@UiController("itpearls_InternalEmailTemplate.edit")
@UiDescriptor("internal-email-template-edit.xml")
@EditedEntityContainer("internalEmailTemplateDc")
@LoadDataBeforeShow
public class InternalEmailTemplateEdit extends StandardEditor<InternalEmailTemplate> {
}