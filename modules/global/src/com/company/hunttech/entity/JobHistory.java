package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@NamePattern("%s %s %s|candidate,currentCompany,currentPosition")
@Table(name = "HUNTTECH_JOB_HISTORY")
@Entity(name = "hunttech_JobHistory")
public class JobHistory extends StandardEntity {
    private static final long serialVersionUID = 469235897178379167L;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CANDIDATE_ID")
    protected JobCandidate candidate;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CURRENT_POSITION_ID")
    protected Position currentPosition;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CURRENT_COMPANY_ID")
    protected Company currentCompany;

    @Temporal(TemporalType.DATE)
    @Column(name = "DATE_NEWS_POSITION")
    protected Date dateNewsPosition;

    @Temporal(TemporalType.DATE)
    @Column(name = "START_DATE")
    protected Date startDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "END_DATE")
    protected Date endDate;

    @Lob
    @Column(name = "DUTIES")
    protected String duties;

    @Column(name = "RAW_POSITION_NAME", length = 255)
    protected String rawPositionName;

    @Column(name = "RAW_COMPANY_NAME", length = 255)
    protected String rawCompanyName;

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getDuties() {
        return duties;
    }

    public void setDuties(String duties) {
        this.duties = duties;
    }

    public String getRawPositionName() {
        return rawPositionName;
    }

    public void setRawPositionName(String rawPositionName) {
        this.rawPositionName = rawPositionName;
    }

    public String getRawCompanyName() {
        return rawCompanyName;
    }

    public void setRawCompanyName(String rawCompanyName) {
        this.rawCompanyName = rawCompanyName;
    }

    public Date getDateNewsPosition() {
        return dateNewsPosition;
    }

    public void setDateNewsPosition(Date dateNewsPosition) {
        this.dateNewsPosition = dateNewsPosition;
    }

    public Company getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(Company currentCompany) {
        this.currentCompany = currentCompany;
    }

    public Position getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(Position currentPosition) {
        this.currentPosition = currentPosition;
    }

    public JobCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(JobCandidate candidate) {
        this.candidate = candidate;
    }
}