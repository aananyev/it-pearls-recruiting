package com.company.itpearls.web.screens.jobcandidate.jobcandidatecomments;

import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.web.screens.fragments.jobcandidatecommentfragment.JobCandidateCommentFragment;
import com.haulmont.cuba.gui.Fragments;
import com.haulmont.cuba.gui.components.ScrollBoxLayout;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("itpearls_JobCandidateComment")
@UiDescriptor("job-candidate-comment.xml")
public class JobCandidateComment extends Screen {
    @Inject
    private CollectionLoader<IteractionList> candidateCommentDl;
    
    private JobCandidate jobCandidate;
    @Inject
    private CollectionContainer<IteractionList> candidateCommentDc;
    @Inject
    private Fragments fragments;
    @Inject
    private ScrollBoxLayout commentScrollBox;

    private Boolean completeSetJobCandidateFlag = false;
    private Boolean getCompleteSetJobCandidateScreenFlag = false;

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
        setJobCandidateCollection(); 
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        setJobCandidateCollection();
        setJobCandidateCommentList();
    }

    private void setJobCandidateCommentList() {
        if (!getCompleteSetJobCandidateScreenFlag) {
            for (IteractionList iteractionList : candidateCommentDc.getItems()) {
                JobCandidateCommentFragment fragment = fragments.create(this, JobCandidateCommentFragment.class);
                fragment.setIteractionList(iteractionList);
                commentScrollBox.add(fragment.getFragment());
            }

            getCompleteSetJobCandidateScreenFlag = true;
        }
    }

    private void setJobCandidateCollection() {
        if (this.jobCandidate != null) {
            if (!completeSetJobCandidateFlag) {
                candidateCommentDl.setParameter("jobCandidate", this.jobCandidate);
                candidateCommentDl.load();
                completeSetJobCandidateFlag = true;
            }
        }
    }

    public void closeScreenButton() {
        closeWithDefaultAction();
    }
}