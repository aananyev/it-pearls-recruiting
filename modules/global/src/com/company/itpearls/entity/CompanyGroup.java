package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.List;

@Table(name = "ITPEARLS_COMPANY_GROUP")
@Entity(name = "itpearls_CompanyGroup")
public class CompanyGroup extends StandardEntity {
    private static final long serialVersionUID = -3791667942121253157L;

    @NotNull
    @Column(name = "COMPANY_RU_GROUP_NAME", nullable = false, unique = true, length = 80)
    protected String companyRuGroupName;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "companyGroup")
    protected List<Company> groupOfCompany;

    public List<Company> getGroupOfCompany() {
        return groupOfCompany;
    }

    public void setGroupOfCompany(List<Company> groupOfCompany) {
        this.groupOfCompany = groupOfCompany;
    }

    public String getCompanyRuGroupName() {
        return companyRuGroupName;
    }

    public void setCompanyRuGroupName(String companyRuGroupName) {
        this.companyRuGroupName = companyRuGroupName;
    }
}