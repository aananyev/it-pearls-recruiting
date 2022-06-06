package com.company.itpearls.web.screens.interview;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Interview;

@UiController("itpearls_Interview.edit")
@UiDescriptor("interview-edit.xml")
@EditedEntityContainer("interviewDc")
@LoadDataBeforeShow
public class InterviewEdit extends StandardEditor<Interview> {
}