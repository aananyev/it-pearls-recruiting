package com.company.itpearls.web.screens.jobcandidate;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;

@UiController("itpearls_JobCandidateSearch.edit")
@UiDescriptor("job-candidate-search-edit.xml")
@EditedEntityContainer("jobCandidateDc")
@LoadDataBeforeShow
public class JobCandidateSearchEdit extends StandardEditor<JobCandidate> {
}