package com.company.itpearls.web.screens.companygroup;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CompanyGroup;

@UiController("itpearls_CompanyGroup.browse")
@UiDescriptor("company-group-browse.xml")
@LookupComponent("companyGroupsTable")
@LoadDataBeforeShow
public class CompanyGroupBrowse extends StandardLookup<CompanyGroup> {
}