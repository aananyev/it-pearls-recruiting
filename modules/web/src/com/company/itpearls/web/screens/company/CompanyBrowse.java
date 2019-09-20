package com.company.itpearls.web.screens.company;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Company;

@UiController("itpearls_Company.browse")
@UiDescriptor("company-browse.xml")
@LookupComponent("companiesTable")
@LoadDataBeforeShow
public class CompanyBrowse extends StandardLookup<Company> {
}