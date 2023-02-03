package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import java.util.Date;

@Table(name = "ITPEARLS_PERSONEL_RESERVE")
@Entity(name = "itpearls_PersonelReserve")
public class PersonelReserve extends StandardEntity {
    private static final long serialVersionUID = 3132402819849167563L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JOB_CANDIDATE_ID")
    private JobCandidate jobCandidate;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REACTUTIER_ID")
    private User rectutier;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE_")
    private Date date;

    @Column(name = "TERM_OF_PLACEMENT")
    private Integer termOfPlacement;

    @Temporal(TemporalType.DATE)
    @Column(name = "END_DATE")
    private Date endDate;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERSON_POSITION_ID")
    private Position personPosition;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup", "open", "clear"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPEN_POSITION_ID")
    private OpenPosition openPosition;

    @Column(name = "REMOVED_FROM_RESERVE")
    private Boolean removedFromReserve;

    @Column(name = "IN_PROCESSED")
    private Boolean inProcess;

    public Boolean getInProcess() {
        return inProcess;
    }

    public void setInProcess(Boolean inProcess) {
        this.inProcess = inProcess;
    }

    public Boolean getRemovedFromReserve() {
        return removedFromReserve;
    }

    public void setRemovedFromReserve(Boolean removedFromReserve) {
        this.removedFromReserve = removedFromReserve;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public Position getPersonPosition() {
        return personPosition;
    }

    public void setPersonPosition(Position personPosition) {
        this.personPosition = personPosition;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Integer getTermOfPlacement() {
        return termOfPlacement;
    }

    public void setTermOfPlacement(Integer termOfPlacement) {
        this.termOfPlacement = termOfPlacement;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public User getRectutier() {
        return rectutier;
    }

    public void setRectutier(User rectutier) {
        this.rectutier = rectutier;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }
}