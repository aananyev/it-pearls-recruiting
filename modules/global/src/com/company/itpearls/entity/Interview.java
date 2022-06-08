package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Table(name = "ITPEARLS_INTERVIEW")
@Entity(name = "itpearls_Interview")
public class Interview extends StandardEntity {
    private static final long serialVersionUID = 2948056086475891984L;

    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    @Column(name = "DATE_INTERVIEW", nullable = false)
    private Date dateInterview;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup", "open", "clear"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CANDIDATE_ID")
    private JobCandidate candidate;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup", "open", "clear"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CANDIDATE_CV_ID")
    private CandidateCV candidateCV;

    @ManyToMany
    @JoinTable(name = "ITPEARLS_INTERVIEW_OPEN_POSITION_LINK",
            joinColumns = @JoinColumn(name = "INTERVIEW_ID"),
            inverseJoinColumns = @JoinColumn(name = "OPEN_POSITION_ID"))
    private List<OpenPosition> openPositions;

    @Lob
    @Column(name = "LETTER")
    private String letter;

    @Lob
    @Column(name = "LETTER_REQUIREMENTS")
    private String letterRequirements;

    @Lob
    @Column(name = "NOTE")
    private String note;

    @Column(name = "SALARY_MIN")
    private Integer salaryMin;

    @Column(name = "SALARY_MAX")
    private Integer salaryMax;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup", "clear"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RECRUTIER_ID")
    private User recrutier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESEARCHER_ID")
    private User researcher;

    public User getResearcher() {
        return researcher;
    }

    public void setResearcher(User researcher) {
        this.researcher = researcher;
    }

    public User getRecrutier() {
        return recrutier;
    }

    public void setRecrutier(User recrutier) {
        this.recrutier = recrutier;
    }

    public String getLetterRequirements() {
        return letterRequirements;
    }

    public void setLetterRequirements(String letterRequirements) {
        this.letterRequirements = letterRequirements;
    }

    public Integer getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(Integer salaryMax) {
        this.salaryMax = salaryMax;
    }

    public Integer getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(Integer salaryMin) {
        this.salaryMin = salaryMin;
    }

    public void setOpenPositions(List<OpenPosition> openPositions) {
        this.openPositions = openPositions;
    }

    public List<OpenPosition> getOpenPositions() {
        return openPositions;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getLetter() {
        return letter;
    }

    public void setLetter(String letter) {
        this.letter = letter;
    }

    public CandidateCV getCandidateCV() {
        return candidateCV;
    }

    public void setCandidateCV(CandidateCV candidateCV) {
        this.candidateCV = candidateCV;
    }

    public JobCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(JobCandidate candidate) {
        this.candidate = candidate;
    }

    public Date getDateInterview() {
        return dateInterview;
    }

    public void setDateInterview(Date dateInterview) {
        this.dateInterview = dateInterview;
    }
}