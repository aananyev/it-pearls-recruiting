package com.company.itpearls.web.screens.internalemailertemplate;

import com.company.itpearls.entity.InternalEmailer;
import com.company.itpearls.web.screens.internalemailer.InternalEmailerEdit;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.InternalEmailerTemplate;

@UiController("itpearls_InternalEmailerTemplate.edit")
@UiDescriptor("internal-emailer-template-edit.xml")
@EditedEntityContainer("internalEmailerTemplateDc")
@LoadDataBeforeShow
public class InternalEmailerTemplateEdit extends InternalEmailerEdit<InternalEmailer> {
}