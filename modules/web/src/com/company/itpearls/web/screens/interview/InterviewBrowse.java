package com.company.itpearls.web.screens.interview;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Interview;

@UiController("itpearls_Interview.browse")
@UiDescriptor("interview-browse.xml")
@LookupComponent("interviewsTable")
@LoadDataBeforeShow
public class InterviewBrowse extends StandardLookup<Interview> {
}