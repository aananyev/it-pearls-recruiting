package com.company.itpearls.web.screens.companydepartament;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CompanyDepartament;

@UiController("itpearls_CompanyDepartament.browse")
@UiDescriptor("company-departament-browse.xml")
@LookupComponent("companyDepartamentsTable")
@LoadDataBeforeShow
public class CompanyDepartamentBrowse extends StandardLookup<CompanyDepartament> {
}