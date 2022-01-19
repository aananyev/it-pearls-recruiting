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

    @Column(name = "AGREEMENT_NAME")
    private String agreementName;

    @Column(name = "AGREEMENT_NUMBER", length = 48)
    private String agreementNumber;

    @Temporal(TemporalType.DATE)
    @Column(name = "AGREEMENT_DATE")
    private Date agreementDate;

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