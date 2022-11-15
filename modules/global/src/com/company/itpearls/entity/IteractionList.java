package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

@Table(name = "ITPEARLS_ITERACTION_LIST", indexes = {
        @Index(name = "IDX_ITPEARLS_ITERACTION_LIST_NUMBER", columnList = "NUMBER_ITERACTION"),
        @Index(name = "IDX_ITPEARLS_ITERACTION_LIST_DATE", columnList = "DATE_ITERACTION")
})
@Entity(name = "itpearls_IteractionList")
@NamePattern("%s|candidate")
public class IteractionList extends StandardEntity {
    private static final long serialVersionUID = -1889183300534377752L;

    @Column(name = "NUMBER_ITERACTION")
    protected BigDecimal numberIteraction;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITERACTION_TYPE_ID")
    protected Iteraction iteractionType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE_ITERACTION")
    protected Date dateIteraction;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CANDIDATE_ID")
    protected JobCandidate candidate;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VACANCY_ID")
    protected OpenPosition vacancy;

    //    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
//    @NotNull
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "COMPANY_DEPARTMENT_ID")
//    protected CompanyDepartament companyDepartment;

    @Column(name = "COMMUNICATION_METHOD", length = 80)
    protected String communicationMethod;

    @Lob
    @Column(name = "COMMENT_")
    protected String comment;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RECRUTIER_ID")
    protected User recrutier;

    @Column(name = "RECRUTIER_NAME", length = 80)
    protected String recrutierName;

    @Column(name = "ADD_TYPE")
    protected Integer addType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ADD_DATE")
    protected Date addDate;

    @Column(name = "ADD_STRING")
    protected String addString;

    @Column(name = "ADD_INTEGER")
    protected Integer addInteger;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @OnDelete(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LEGAL_ENTITY_ID")
    private LaborAgreement laborAgreement;

    @Column(name = "RATING")
    protected Integer rating;

    @Column(name = "CURRENT_PRIORITY")
    private Integer currentPriority;

    @Column(name = "CURRENT_OPEN_CLOSE")
    private Boolean currentOpenClose;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CHAIN_INTERACTION_ID")
    private IteractionList chainInteraction;

    public Boolean getCurrentOpenClose() {
        return currentOpenClose;
    }

    public void setCurrentOpenClose(Boolean currentOpenClose) {
        this.currentOpenClose = currentOpenClose;
    }

    public IteractionList getChainInteraction() {
        return chainInteraction;
    }

    public void setChainInteraction(IteractionList chainInteraction) {
        this.chainInteraction = chainInteraction;
    }

    public Integer getCurrentPriority() {
        return currentPriority;
    }

    public void setCurrentPriority(Integer currentPriority) {
        this.currentPriority = currentPriority;
    }

    public LaborAgreement getLaborAgreement() {
        return laborAgreement;
    }

    public void setLaborAgreement(LaborAgreement laborAgreement) {
        this.laborAgreement = laborAgreement;
    }

    public Integer getAddInteger() {
        return addInteger;
    }

    public void setAddInteger(Integer addInteger) {
        this.addInteger = addInteger;
    }

    public String getAddString() {
        return addString;
    }

    public void setAddString(String addString) {
        this.addString = addString;
    }

    public Integer getAddType() {
        return addType;
    }

    public void setAddType(Integer addType) {
        this.addType = addType;
    }

    public Date getAddDate() {
        return addDate;
    }

    public void setAddDate(Date addDate) {
        this.addDate = addDate;
    }

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

//    public CompanyDepartament getCompanyDepartment() {
//        return companyDepartment;
//    }

//    public void setCompanyDepartment(CompanyDepartament companyDepartment) {
//        this.companyDepartment = companyDepartment;
//    }

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

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getRating() {
        return rating;
    }
}
