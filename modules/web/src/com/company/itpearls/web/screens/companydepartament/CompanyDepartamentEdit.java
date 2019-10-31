package com.company.itpearls.web.screens.companydepartament;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CompanyDepartament;

@UiController("itpearls_CompanyDepartament.edit")
@UiDescriptor("company-departament-edit.xml")
@EditedEntityContainer("companyDepartamentDc")
@LoadDataBeforeShow
public class CompanyDepartamentEdit extends StandardEditor<CompanyDepartament> {
}