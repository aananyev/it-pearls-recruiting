package com.company.itpearls.web.screens.jobcandidate;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;

@UiController("itpearls_JobCandidate.browse")
@UiDescriptor("job-candidate-browse.xml")
@LookupComponent("jobCandidatesTable")
@LoadDataBeforeShow
public class JobCandidateBrowse extends StandardLookup<JobCandidate> {
}