package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Table(name = "ITPEARLS_CANDIDATE_CV_WORK_PLACES")
@Entity(name = "itpearls_CandidateCVWorkPlaces")
public class CandidateCVWorkPlaces extends StandardEntity {
    private static final long serialVersionUID = -122028494660854545L;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CANDIDATE_CV_ID")
    private CandidateCV candidateCV;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WORK_PLACE_ID")
    private Company workPlace;

    @Column(name = "WORK_PLACE_COMMENT", length = 128)
    private String workPlaceComment;

    @Temporal(TemporalType.DATE)
    @NotNull
    @Column(name = "START_DATE", nullable = false)
    private Date startDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "END_DATE")
    private Date endDate;

    @Column(name = "WORK_TO_THIS_DAY")
    private Boolean workToThisDay;

    @Lob
    @Column(name = "FUNCTIONALITY_AT_WORK")
    private String functionalityAtWork;

    @Lob
    @Column(name = "PERSONAL_ROLE")
    private String personalRole;

    @Lob
    @Column(name = "ACHIEVEMENTS")
    private String achievements;

    public String getWorkPlaceComment() {
        return workPlaceComment;
    }

    public void setWorkPlaceComment(String workPlaceComment) {
        this.workPlaceComment = workPlaceComment;
    }

    public String getAchievements() {
        return achievements;
    }

    public void setAchievements(String achievements) {
        this.achievements = achievements;
    }

    public String getPersonalRole() {
        return personalRole;
    }

    public void setPersonalRole(String personalRole) {
        this.personalRole = personalRole;
    }

    public String getFunctionalityAtWork() {
        return functionalityAtWork;
    }

    public void setFunctionalityAtWork(String functionalityAtWork) {
        this.functionalityAtWork = functionalityAtWork;
    }

    public Boolean getWorkToThisDay() {
        return workToThisDay;
    }

    public void setWorkToThisDay(Boolean workToThisDay) {
        this.workToThisDay = workToThisDay;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Company getWorkPlace() {
        return workPlace;
    }

    public void setWorkPlace(Company workPlace) {
        this.workPlace = workPlace;
    }

    public CandidateCV getCandidateCV() {
        return candidateCV;
    }

    public void setCandidateCV(CandidateCV candidateCV) {
        this.candidateCV = candidateCV;
    }
}