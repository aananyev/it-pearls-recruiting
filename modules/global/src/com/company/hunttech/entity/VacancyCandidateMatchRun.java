package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import java.util.Date;

@Table(name = "HUNTTECH_VACANCY_CANDIDATE_MATCH_RUN")
@Entity(name = "hunttech_VacancyCandidateMatchRun")
@NamePattern("%s %s|vacancy,runTime")
public class VacancyCandidateMatchRun extends StandardEntity {
    private static final long serialVersionUID = -249120984128941249L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPEN_POSITION_ID")
    protected OpenPosition vacancy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JOB_CANDIDATE_ID")
    protected JobCandidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RECRUITER_ID")
    protected User recruiter;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "RUN_TIME")
    protected Date runTime;

    @Column(name = "CONFIGURATION_VERSION")
    protected Integer configurationVersion;

    @Column(name = "AI_FUNCTION_CODE", length = 64)
    protected String aiFunctionCode;

    @Column(name = "CANDIDATES_REVIEWED_COUNT")
    protected Integer candidatesReviewedCount;

    @Column(name = "TAKEN_INTO_WORK_COUNT")
    protected Integer takenIntoWorkCount;

    @Column(name = "REJECTED_COUNT")
    protected Integer rejectedCount;

    @Column(name = "POSTPONED_COUNT")
    protected Integer postponedCount;

    @Column(name = "OUTREACH_DRAFTS_COUNT")
    protected Integer outreachDraftsGeneratedCount;

    public OpenPosition getVacancy() {
        return vacancy;
    }

    public void setVacancy(OpenPosition vacancy) {
        this.vacancy = vacancy;
    }

    public JobCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(JobCandidate candidate) {
        this.candidate = candidate;
    }

    public User getRecruiter() {
        return recruiter;
    }

    public void setRecruiter(User recruiter) {
        this.recruiter = recruiter;
    }

    public Date getRunTime() {
        return runTime;
    }

    public void setRunTime(Date runTime) {
        this.runTime = runTime;
    }

    public Integer getConfigurationVersion() {
        return configurationVersion;
    }

    public void setConfigurationVersion(Integer configurationVersion) {
        this.configurationVersion = configurationVersion;
    }

    public String getAiFunctionCode() {
        return aiFunctionCode;
    }

    public void setAiFunctionCode(String aiFunctionCode) {
        this.aiFunctionCode = aiFunctionCode;
    }

    public Integer getCandidatesReviewedCount() {
        return candidatesReviewedCount;
    }

    public void setCandidatesReviewedCount(Integer candidatesReviewedCount) {
        this.candidatesReviewedCount = candidatesReviewedCount;
    }

    public Integer getTakenIntoWorkCount() {
        return takenIntoWorkCount;
    }

    public void setTakenIntoWorkCount(Integer takenIntoWorkCount) {
        this.takenIntoWorkCount = takenIntoWorkCount;
    }

    public Integer getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(Integer rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public Integer getPostponedCount() {
        return postponedCount;
    }

    public void setPostponedCount(Integer postponedCount) {
        this.postponedCount = postponedCount;
    }

    public Integer getOutreachDraftsGeneratedCount() {
        return outreachDraftsGeneratedCount;
    }

    public void setOutreachDraftsGeneratedCount(Integer outreachDraftsGeneratedCount) {
        this.outreachDraftsGeneratedCount = outreachDraftsGeneratedCount;
    }
}
