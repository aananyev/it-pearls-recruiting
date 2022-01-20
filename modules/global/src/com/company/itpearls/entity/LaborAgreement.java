package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Table(name = "ITPEARLS_LABOR_AGREEMENT")
@Entity(name = "itpearls_LaborAgreement")
@NamePattern("%s|laborAgreementType")
public class LaborAgreement extends StandardEntity {
    private static final long serialVersionUID = 2987525215308840854L;

    @Column(name = "PERHAPS")
    private Boolean perhaps;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup", "open"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "JOB_CANDIDATE_ID")
    @NotNull
    private JobCandidate jobCandidate;

    @Column(name = "AGREEMENT_NAME", nullable = false)
    @NotNull
    private String agreementName;

    @Column(name = "AGREEMENT_NUMBER", nullable = false, length = 48)
    @NotNull
    private String agreementNumber;

    @Temporal(TemporalType.DATE)
    @Column(name = "AGREEMENT_DATE", nullable = false)
    @NotNull
    private Date agreementDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "AGREEMENT_END_DATE")
    private Date agreementEndDate;

    @Column(name = "PERPETUAL_AGREEMENT")
    private Boolean perpetualAgreement;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @OnDelete(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COMPANY_ID")
    @NotNull
    private Company company;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @OnDelete(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LABOR_AGREEMENT_TYPE_ID")
    @NotNull
    private LaborAgeementType laborAgreementType;

    @JoinTable(name = "ITPEARLS_OPEN_POSITION_LABOR_AGREEMENT_LINK",
            joinColumns = @JoinColumn(name = "LABOR_AGREEMENT_ID"),
            inverseJoinColumns = @JoinColumn(name = "OPEN_POSITION_ID"))
    @ManyToMany
    private List<OpenPosition> openPositions;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    @Lob
    @Column(name = "AGREEMENT_TEXT")
    private String agreementText;

    public String getAgreementText() {
        return agreementText;
    }

    public void setAgreementText(String agreementText) {
        this.agreementText = agreementText;
    }

    public Boolean getPerpetualAgreement() {
        return perpetualAgreement;
    }

    public void setPerpetualAgreement(Boolean perpetualAgreement) {
        this.perpetualAgreement = perpetualAgreement;
    }

    public Date getAgreementEndDate() {
        return agreementEndDate;
    }

    public void setAgreementEndDate(Date agreementEndDate) {
        this.agreementEndDate = agreementEndDate;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public Date getAgreementDate() {
        return agreementDate;
    }

    public void setAgreementDate(Date agreementDate) {
        this.agreementDate = agreementDate;
    }

    public String getAgreementNumber() {
        return agreementNumber;
    }

    public void setAgreementNumber(String agreementNumber) {
        this.agreementNumber = agreementNumber;
    }

    public String getAgreementName() {
        return agreementName;
    }

    public void setAgreementName(String agreementName) {
        this.agreementName = agreementName;
    }

    public Boolean getPerhaps() {
        return perhaps;
    }

    public void setPerhaps(Boolean perhaps) {
        this.perhaps = perhaps;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<OpenPosition> getOpenPositions() {
        return openPositions;
    }

    public void setOpenPositions(List<OpenPosition> openPositions) {
        this.openPositions = openPositions;
    }

    public LaborAgeementType getLaborAgreementType() {
        return laborAgreementType;
    }

    public void setLaborAgreementType(LaborAgeementType laborAgreementType) {
        this.laborAgreementType = laborAgreementType;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}