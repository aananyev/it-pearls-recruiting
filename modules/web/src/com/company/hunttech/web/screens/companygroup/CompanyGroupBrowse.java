package com.company.hunttech.web.screens.companygroup;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.CompanyGroup;

@UiController("hunttech_CompanyGroup.browse")
@UiDescriptor("company-group-browse.xml")
@LookupComponent("companyGroupsTable")
@LoadDataBeforeShow
public class CompanyGroupBrowse extends StandardLookup<CompanyGroup> {
}