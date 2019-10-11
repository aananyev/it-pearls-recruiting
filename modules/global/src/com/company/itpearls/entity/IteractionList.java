package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

@Table(name = "ITPEARLS_ITERACTION_LIST")
@Entity(name = "itpearls_IteractionList")
@NamePattern("%s|candidate")
public class IteractionList extends StandardEntity {
    private static final long serialVersionUID = -1889183300534377752L;

    @Column(name = "NUMBER_ITERACTION")
    protected BigDecimal numberIteraction;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE_ITERACTION")
    protected Date dateIteraction;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CANDIDATE_ID")
    protected JobCandidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CURRENT_JOB_POSITION_ID")
    protected Position currentJobPosition;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VACANCY_ID")
    protected OpenPosition vacancy;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_ID")
    protected Project project;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COMPANY_DEPARTMENT_ID")
    protected CompanyDepartament companyDepartment;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ITERACTION_TYPE_ID")
    protected Iteraction iteractionType;

    @Column(name = "COMMUNICATION_METHOD", length = 80)
    protected String communicationMethod;

    @Lob
    @Column(name = "COMMENT_")
    protected String comment;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RECRUTIER_ID")
    protected User recrutier;

    @Column(name = "RECRUTIER_NAME", length = 80)
    protected String recrutierName;

    public String getRecrutierName() {
        return recrutierName;
    }

    public void setRecrutierName(String recrutierName) {
        this.recrutierName = recrutierName;
    }

    public OpenPosition getVacancy() {
        return vacancy;
    }

    public void setVacancy(OpenPosition vacancy) {
        this.vacancy = vacancy;
    }

    public void setNumberIteraction(BigDecimal numberIteraction) {
        this.numberIteraction = numberIteraction;
    }

    public BigDecimal getNumberIteraction() {
        return numberIteraction;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCommunicationMethod() {
        return communicationMethod;
    }

    public void setCommunicationMethod(String communicationMethod) {
        this.communicationMethod = communicationMethod;
    }

    public User getRecrutier() {
        return recrutier;
    }

    public void setRecrutier(User recrutier) {
        this.recrutier = recrutier;
    }

    public Iteraction getIteractionType() {
        return iteractionType;
    }

    public void setIteractionType(Iteraction iteractionType) {
        this.iteractionType = iteractionType;
    }

    public CompanyDepartament getCompanyDepartment() {
        return companyDepartment;
    }

    public void setCompanyDepartment(CompanyDepartament companyDepartment) {
        this.companyDepartment = companyDepartment;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Position getCurrentJobPosition() {
        return currentJobPosition;
    }

    public JobCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(JobCandidate candidate) {
        this.candidate = candidate;
    }

    public Date getDateIteraction() {
        return dateIteraction;
    }

    public void setDateIteraction(Date dateIteraction) {
        this.dateIteraction = dateIteraction;
    }

}