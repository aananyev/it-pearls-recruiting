package com.company.itpearls.web.screens.applicationrecruitment;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.ApplicationRecruitment;

@UiController("itpearls_ApplicationRecruitment.edit")
@UiDescriptor("application-recruitment-edit.xml")
@EditedEntityContainer("applicationRecruitmentDc")
@LoadDataBeforeShow
public class ApplicationRecruitmentEdit extends StandardEditor<ApplicationRecruitment> {
}