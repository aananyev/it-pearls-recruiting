package com.company.hunttech.web.screens.companydepartament;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.CompanyDepartament;

@UiController("hunttech_CompanyDepartament.browse")
@UiDescriptor("company-departament-browse.xml")
@LookupComponent("companyDepartamentsTable")
@LoadDataBeforeShow
public class CompanyDepartamentBrowse extends StandardLookup<CompanyDepartament> {
}