package com.company.hunttech.web.screens.companygroup;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.CompanyGroup;

@UiController("hunttech_CompanyGroup.edit")
@UiDescriptor("company-group-edit.xml")
@EditedEntityContainer("companyGroupDc")
@LoadDataBeforeShow
public class CompanyGroupEdit extends StandardEditor<CompanyGroup> {
}