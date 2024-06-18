package com.company.itpearls.web.screens.jobcandidate.jobcandidatepartners;

import com.company.itpearls.core.PartnerPersonService;
import com.company.itpearls.entity.JobCandidatePartners;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.model.CollectionLoader;
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
    @Inject
    private CollectionLoader<JobCandidatePartners> jobCandidatesDl;
    @Inject
    private PartnerPersonService partnerPersonService;

    @Subscribe
    public void onAfterShow1(AfterShowEvent event) {
        buttonExcel.setVisible(false);

        setPartnersFilter();
    }

    private void setPartnersFilter() {
        jobCandidatesDl.setParameter("partner", partnerPersonService.getMyPartner());
        jobCandidatesDl.load();
    }
}