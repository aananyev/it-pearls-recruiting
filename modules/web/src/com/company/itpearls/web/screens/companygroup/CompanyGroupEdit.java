package com.company.itpearls.web.screens.companygroup;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CompanyGroup;

@UiController("itpearls_CompanyGroup.edit")
@UiDescriptor("company-group-edit.xml")
@EditedEntityContainer("companyGroupDc")
@LoadDataBeforeShow
public class CompanyGroupEdit extends StandardEditor<CompanyGroup> {
}