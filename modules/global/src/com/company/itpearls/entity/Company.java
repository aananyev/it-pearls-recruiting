package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@NamePattern("%s|comanyName")
@Table(name = "ITPEARLS_COMPANY")
@Entity(name = "itpearls_Company")
public class Company extends StandardEntity {
    private static final long serialVersionUID = 7912366724901851184L;

    @Column(name = "COMPANY_OUR_CLIENT")
    protected Boolean companyOurClient;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_OWNERSHIP_ID")
    protected Ownershup companyOwnership;

    @NotNull
    @Column(name = "COMANY_NAME", nullable = false, unique = true, length = 80)
    protected String comanyName;

    @Column(name = "COMPANY_SHORT_NAME", unique = true, length = 30)
    protected String companyShortName;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_DIRECTOR_ID")
    protected Person companyDirector;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "companyName")
    protected List<CompanyDepartament> departmentOfCompany;

    public List<CompanyDepartament> getDepartmentOfCompany() {
        return departmentOfCompany;
    }

    public void setDepartmentOfCompany(List<CompanyDepartament> departmentOfCompany) {
        this.departmentOfCompany = departmentOfCompany;
    }

    public Boolean getCompanyOurClient() {
        return companyOurClient;
    }

    public void setCompanyOurClient(Boolean companyOurClient) {
        this.companyOurClient = companyOurClient;
    }

    public Person getCompanyDirector() {
        return companyDirector;
    }

    public void setCompanyDirector(Person companyDirector) {
        this.companyDirector = companyDirector;
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