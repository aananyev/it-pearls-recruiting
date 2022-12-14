package com.company.itpearls.web.screens.applicationrecruitment;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.ApplicationRecruitment;

@UiController("itpearls_ApplicationRecruitment.browse")
@UiDescriptor("application-recruitment-browse.xml")
@LookupComponent("applicationRecruitmentsTable")
@LoadDataBeforeShow
public class ApplicationRecruitmentBrowse extends StandardLookup<ApplicationRecruitment> {
}