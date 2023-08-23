package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_MY_ACTIVE_CANDIDATE_EXCLUDE")
@Entity(name = "itpearls_MyActiveCandidateExclude")
public class MyActiveCandidateExclude extends StandardEntity {
    private static final long serialVersionUID = 643840158144482119L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "JOB_CANDIDATE_ID")
    private JobCandidate jobCandidate;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPEN_POSITION_ID")
    private OpenPosition openPosition;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID")
    private ExtUser user;

    public ExtUser getUser() {
        return user;
    }

    public void setUser(ExtUser user) {
        this.user = user;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }
}