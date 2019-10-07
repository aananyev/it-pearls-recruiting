package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@NamePattern("%s|candidate")
@Table(name = "ITPEARLS_CANDIDATE_CV")
@Entity(name = "itpearls_CandidateCV")
public class CandidateCV extends StandardEntity {
    private static final long serialVersionUID = 7346397128043882179L;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CANDIDATE_ID")
    protected JobCandidate candidate;

    @Lob
    @Column(name = "TEXT_CV")
    protected String textCV;

    @Temporal(TemporalType.DATE)
    @NotNull
    @Column(name = "DATE_POST", nullable = false)
    protected Date datePost;

    public Date getDatePost() {
        return datePost;
    }

    public void setDatePost(Date datePost) {
        this.datePost = datePost;
    }

    public String getTextCV() {
        return textCV;
    }

    public void setTextCV(String textCV) {
        this.textCV = textCV;
    }

    public JobCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(JobCandidate candidate) {
        this.candidate = candidate;
    }
}