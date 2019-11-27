package com.company.itpearls.web.screens.jobcandidate;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;

@UiController("itpearls_JobCandidateSearch.browse")
@UiDescriptor("job-candidate-search.xml")
@LookupComponent("jobCandidatesTable")
@LoadDataBeforeShow
public class JobCandidateSearch extends StandardLookup<JobCandidate> {
}