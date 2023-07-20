package com.company.itpearls.web.screens.jobcandidate;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;

@UiController("itpearls_JobCandidateInProject.browse")
@UiDescriptor("job-candidate-in-project-browse.xml")
@LookupComponent("jobCandidatesTable")
@LoadDataBeforeShow
public class JobCandidateInProjectBrowse extends StandardLookup<JobCandidate> {
}