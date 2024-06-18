package com.company.itpearls.web.screens.jobcandidate.jobcandidatepartners;

import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.jobcandidate.JobCandidateBrowse;

import javax.inject.Inject;

@UiController("itpearls_JobCandidatePartners.browse")
@UiDescriptor("job-candidate-partners-browse.xml")
public class JobCandidatePartnersBrowse extends JobCandidateBrowse {
    @Inject
    private Button buttonExcel;

    @Subscribe
    public void onAfterShow1(AfterShowEvent event) {
        buttonExcel.setVisible(false);
    }
}