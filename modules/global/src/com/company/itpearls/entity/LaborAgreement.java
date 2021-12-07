package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;

@Table(name = "ITPEARLS_LABOR_AGREEMENT")
@Entity(name = "itpearls_LaborAgreement")
@NamePattern("%s %s|laborAgreementType,company")
public class LaborAgreement extends StandardEntity {
    private static final long serialVersionUID = 2987525215308840854L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @OnDelete(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_ID")
    private Company company;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @OnDelete(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LABOR_AGREEMENT_TYPE_ID")
    private LaborAgeementType laborAgreementType;

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