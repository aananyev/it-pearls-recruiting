package com.company.hunttech.web.screens.jobhistory;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.JobHistory;

@UiController("hunttech_JobHistory.edit")
@UiDescriptor("job-history-edit.xml")
@EditedEntityContainer("jobHistoryDc")
@LoadDataBeforeShow
public class JobHistoryEdit extends StandardEditor<JobHistory> {
}