package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;

@Table(name = "ITPEARLS_JOB_CANDIDATE_SIGN_ICON")
@Entity(name = "itpearls_JobCandidateSignIcon")
public class JobCandidateSignIcon extends StandardEntity {
    private static final long serialVersionUID = 1390032645510438237L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JOB_CANDIDATE_ID")
    private JobCandidate jobCandidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SIGN_ICON_ID")
    private SignIcons signIcon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private ExtUser user;

    public ExtUser getUser() {
        return user;
    }

    public void setUser(ExtUser user) {
        this.user = user;
    }

    public SignIcons getSignIcon() {
        return signIcon;
    }

    public void setSignIcon(SignIcons signIcon) {
        this.signIcon = signIcon;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }
}