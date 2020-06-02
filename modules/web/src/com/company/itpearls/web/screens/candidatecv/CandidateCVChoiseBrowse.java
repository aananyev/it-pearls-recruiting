package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidateCV;

import javax.inject.Inject;

@UiController("itpearls_CandidateCVChoise.browse")
@UiDescriptor("candidate-cv-choise-browse.xml")
@LookupComponent("candidateCVsTable")
@LoadDataBeforeShow
public class CandidateCVChoiseBrowse extends StandardLookup<CandidateCV> {
    private JobCandidate jobCandidate;
    @Inject
    private CollectionLoader<CandidateCV> candidateCVsDl;

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (jobCandidate != null ) {
            candidateCVsDl.setParameter( "candidate", jobCandidate );
        } else {
            candidateCVsDl.removeParameter( "candidate" );
        }

        candidateCVsDl.load();
    }
}