package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;

@Table(name = "ITPEARLS_JOB_CANDIDATE_POSITION_LISTS")
@Entity(name = "itpearls_JobCandidatePositionLists")
public class JobCandidatePositionLists extends StandardEntity {
    private static final long serialVersionUID = -6708540309881368323L;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POSITION_LIST_ID")
    private Position positionList;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "JOB_CANDIDATE_ID")
    private JobCandidate jobCandidate;

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public Position getPositionList() {
        return positionList;
    }

    public void setPositionList(Position positionList) {
        this.positionList = positionList;
    }

}