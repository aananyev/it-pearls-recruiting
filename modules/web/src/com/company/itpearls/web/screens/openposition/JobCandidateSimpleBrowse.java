package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;
import javax.inject.Inject;

@UiController("itpearls_JobCandidateSimple.browse")
@UiDescriptor("job-candidate-simple-browse.xml")
@LookupComponent("iteractionListsTable")
@LoadDataBeforeShow
public class JobCandidateSimpleBrowse extends StandardLookup<IteractionList> {
    @Inject
    private DataGrid<IteractionList> iteractionListsTable;
    @Inject
    private Label<String> candidateLabel;
    @Inject
    private Label<String> candidatePositionLabel;
    @Inject
    private Label<String> candidatePositionEnLabel;
    @Inject
    private CollectionLoader<JobCandidate> jobCandidateDl;
    JobCandidate jobCandidate = null;

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setCandidateLabel(event);
    }

    private void setCandidateLabel(BeforeShowEvent event) {
        if (jobCandidate != null) {
            candidateLabel.setValue(jobCandidate.getFullName());
            if (jobCandidate.getPersonPosition() != null) {
                if (jobCandidate.getPersonPosition().getPositionRuName() != null) {
                    candidatePositionLabel.setValue(jobCandidate.getPersonPosition().getPositionRuName());
                }

                if (jobCandidate.getPersonPosition().getPositionEnName() != null) {
                    candidatePositionEnLabel.setValue(jobCandidate.getPersonPosition().getPositionEnName());
                }
            }
        }
    }

    private OpenPosition openPosition;

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
        jobCandidateDl.setParameter("vacancy", this.openPosition);
        jobCandidateDl.load();
    }
}