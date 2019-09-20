package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_COMPANY")
@Entity(name = "itpearls_Company")
public class Company extends StandardEntity {
    private static final long serialVersionUID = 7912366724901851184L;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_OWNERSHIP_ID")
    protected Ownershup companyOwnership;

    @NotNull
    @Column(name = "COMANY_NAME", nullable = false, unique = true, length = 80)
    protected String comanyName;

    @Column(name = "COMPANY_SHORT_NAME", unique = true, length = 30)
    protected String companyShortName;

    @Column(name = "COMPANY_EN_NAME", unique = true, length = 80)
    protected String companyEnName;

    @Column(name = "COMPANY_EN_SHORT_NAME", unique = true, length = 30)
    protected String companyEnShortName;

    public String getCompanyEnShortName() {
        return companyEnShortName;
    }

    public void setCompanyEnShortName(String companyEnShortName) {
        this.companyEnShortName = companyEnShortName;
    }

    public String getCompanyEnName() {
        return companyEnName;
    }

    public void setCompanyEnName(String companyEnName) {
        this.companyEnName = companyEnName;
    }

    public String getCompanyShortName() {
        return companyShortName;
    }

    public void setCompanyShortName(String companyShortName) {
        this.companyShortName = companyShortName;
    }

    public Ownershup getCompanyOwnership() {
        return companyOwnership;
    }

    public void setCompanyOwnership(Ownershup companyOwnership) {
        this.companyOwnership = companyOwnership;
    }

    public String getComanyName() {
        return comanyName;
    }

    public void setComanyName(String comanyName) {
        this.comanyName = comanyName;
    }
}