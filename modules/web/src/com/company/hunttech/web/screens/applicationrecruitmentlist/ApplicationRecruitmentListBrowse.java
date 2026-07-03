package com.company.hunttech.web.screens.applicationrecruitmentlist;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.ApplicationRecruitmentList;

@UiController("hunttech_ApplicationRecruitmentList.browse")
@UiDescriptor("application-recruitment-list-browse.xml")
@LookupComponent("applicationRecruitmentListsTable")
@LoadDataBeforeShow
public class ApplicationRecruitmentListBrowse extends StandardLookup<ApplicationRecruitmentList> {
}