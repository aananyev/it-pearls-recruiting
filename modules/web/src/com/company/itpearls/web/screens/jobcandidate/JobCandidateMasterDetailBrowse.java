package com.company.itpearls.web.screens.jobcandidate;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;

@UiController("itpearls_JobCandidateMasterDetail.browse")
@UiDescriptor("job-candidate-master-detail-browse.xml")
@LookupComponent("table")
@LoadDataBeforeShow
public class JobCandidateMasterDetailBrowse extends MasterDetailScreen<JobCandidate> {
}