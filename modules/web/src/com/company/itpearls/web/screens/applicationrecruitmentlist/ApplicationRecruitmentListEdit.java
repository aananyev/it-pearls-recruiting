package com.company.itpearls.web.screens.applicationrecruitmentlist;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.ApplicationRecruitmentList;

@UiController("itpearls_ApplicationRecruitmentList.edit")
@UiDescriptor("application-recruitment-list-edit.xml")
@EditedEntityContainer("applicationRecruitmentListDc")
@LoadDataBeforeShow
public class ApplicationRecruitmentListEdit extends StandardEditor<ApplicationRecruitmentList> {
}