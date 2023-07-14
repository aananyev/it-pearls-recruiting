package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@NamePattern("%s|companyRuGroupName")
@Table(name = "ITPEARLS_COMPANY_GROUP", indexes = {
        @Index(name = "IDX_ITPEARLS_COMPANY_GROUP_RU_NAME", columnList = "COMPANY_RU_GROUP_NAME")
})
@Entity(name = "itpearls_CompanyGroup")
public class CompanyGroup extends StandardEntity {
    private static final long serialVersionUID = -3791667942121253157L;

    @NotNull
    @Column(name = "COMPANY_RU_GROUP_NAME", nullable = false, unique = true, length = 80)
    protected String companyRuGroupName;

    @Composition
    @OneToMany(mappedBy = "companyGroup")
    private List<Company> company;

    public List<Company> getCompany() {
        return company;
    }

    public void setCompany(List<Company> company) {
        this.company = company;
    }

    public String getCompanyRuGroupName() {
        return companyRuGroupName;
    }

    public void setCompanyRuGroupName(String companyRuGroupName) {
        this.companyRuGroupName = companyRuGroupName;
    }
}