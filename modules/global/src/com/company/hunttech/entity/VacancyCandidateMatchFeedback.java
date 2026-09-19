package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Table(name = "HUNTTECH_VACANCY_CANDIDATE_MATCH_FEEDBACK", indexes = {
        @Index(name = "IDX_HUNTTECH_MATCH_FEEDBACK_VACANCY_CANDIDATE", columnList = "OPEN_POSITION_ID, JOB_CANDIDATE_ID"),
        @Index(name = "IDX_HUNTTECH_MATCH_FEEDBACK_RECRUITER", columnList = "RECRUITER_ID, DECISION_TIME")
})
@Entity(name = "hunttech_VacancyCandidateMatchFeedback")
@NamePattern("%s %s %s|candidate,vacancy,decision")
public class VacancyCandidateMatchFeedback extends StandardEntity {
    private static final long serialVersionUID = -712891298412891249L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MATCH_RUN_ID")
    protected VacancyCandidateMatchRun matchRun;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OPEN_POSITION_ID")
    protected OpenPosition vacancy;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "JOB_CANDIDATE_ID")
    protected JobCandidate candidate;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RECRUITER_ID")
    protected User recruiter;

    @Column(name = "AI_SCORE")
    protected Integer aiScore;

    @NotNull
    @Column(name = "DECISION", nullable = false)
    protected Integer decision = RecruiterDecisionType.NEW.getId();

    @Column(name = "REJECTION_REASON", length = 255)
    protected String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITERACTION_TYPE_ID")
    protected Iteraction iteractionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITERACTION_LIST_ID")
    protected IteractionList iteractionList;

    @Lob
    @Column(name = "COMMENT_")
    protected String comment;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DECISION_TIME")
    protected Date decisionTime;

    public VacancyCandidateMatchRun getMatchRun() {
        return matchRun;
    }

    public void setMatchRun(VacancyCandidateMatchRun matchRun) {
        this.matchRun = matchRun;
    }

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

    public Integer getAiScore() {
        return aiScore;
    }

    public void setAiScore(Integer aiScore) {
        this.aiScore = aiScore;
    }

    public RecruiterDecisionType getDecision() {
        return decision != null ? RecruiterDecisionType.fromId(decision) : null;
    }

    public void setDecision(RecruiterDecisionType decision) {
        this.decision = decision != null ? decision.getId() : null;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Iteraction getIteractionType() {
        return iteractionType;
    }

    public void setIteractionType(Iteraction iteractionType) {
        this.iteractionType = iteractionType;
    }

    public IteractionList getIteractionList() {
        return iteractionList;
    }

    public void setIteractionList(IteractionList iteractionList) {
        this.iteractionList = iteractionList;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getDecisionTime() {
        return decisionTime;
    }

    public void setDecisionTime(Date decisionTime) {
        this.decisionTime = decisionTime;
    }
}
