package com.company.itpearls.web.screens.jobhistory;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobHistory;

@UiController("itpearls_JobHistory.browse")
@UiDescriptor("job-history-browse.xml")
@LookupComponent("jobHistoriesTable")
@LoadDataBeforeShow
public class JobHistoryBrowse extends StandardLookup<JobHistory> {
}