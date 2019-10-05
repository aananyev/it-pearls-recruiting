package com.company.itpearls.web.screens.jobhistory;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobHistory;

@UiController("itpearls_JobHistory.edit")
@UiDescriptor("job-history-edit.xml")
@EditedEntityContainer("jobHistoryDc")
@LoadDataBeforeShow
public class JobHistoryEdit extends StandardEditor<JobHistory> {
}