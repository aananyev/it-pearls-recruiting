package com.company.itpearls.web.screens.applicationrecruitmentlist;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.ApplicationRecruitmentList;

@UiController("itpearls_ApplicationRecruitmentList.browse")
@UiDescriptor("application-recruitment-list-browse.xml")
@LookupComponent("applicationRecruitmentListsTable")
@LoadDataBeforeShow
public class ApplicationRecruitmentListBrowse extends StandardLookup<ApplicationRecruitmentList> {
}