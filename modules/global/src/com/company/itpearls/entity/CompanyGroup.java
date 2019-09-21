package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@NamePattern("%s|companyEnGroupName")
@Table(name = "ITPEARLS_COMPANY_GROUP")
@Entity(name = "itpearls_CompanyGroup")
public class CompanyGroup extends StandardEntity {
    private static final long serialVersionUID = -3791667942121253157L;

    @NotNull
    @Column(name = "COMPANY_RU_GROUP_NAME", nullable = false, unique = true, length = 80)
    protected String companyRuGroupName;

    @NotNull
    @Column(name = "COMPANY_EN_GROUP_NAME", nullable = false, unique = true, length = 80)
    protected String companyEnGroupName;

    public String getCompanyEnGroupName() {
        return companyEnGroupName;
    }

    public void setCompanyEnGroupName(String companyEnGroupName) {
        this.companyEnGroupName = companyEnGroupName;
    }

    public String getCompanyRuGroupName() {
        return companyRuGroupName;
    }

    public void setCompanyRuGroupName(String companyRuGroupName) {
        this.companyRuGroupName = companyRuGroupName;
    }
}