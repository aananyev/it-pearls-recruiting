package com.company.hunttech.web.screens.jobhistory;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.JobHistory;

@UiController("hunttech_JobHistory.browse")
@UiDescriptor("job-history-browse.xml")
@LookupComponent("jobHistoriesTable")
@LoadDataBeforeShow
public class JobHistoryBrowse extends StandardLookup<JobHistory> {
}