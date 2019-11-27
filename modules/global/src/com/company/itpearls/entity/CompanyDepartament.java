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

@NamePattern("%s %s|companyName,departamentRuName")
@Table(name = "ITPEARLS_COMPANY_DEPARTAMENT", indexes = {
        @Index(name = "IDX_ITPEARLS_COMPANY_DEPARTAMENT", columnList = "DEPARTAMENT_RU_NAME")
})
@Entity(name = "itpearls_CompanyDepartament")
public class CompanyDepartament extends StandardEntity {
    private static final long serialVersionUID = 2921897445702773073L;

    @NotNull
    @Column(name = "DEPARTAMENT_RU_NAME", nullable = false, length = 80)
    protected String departamentRuName;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_NAME_ID")
    protected Company companyName;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPARTAMENT_HR_DIRECTOR_ID")
    protected Person departamentHrDirector;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPARTAMENT_DIRECTOR_ID")
    protected Person departamentDirector;

    @Lob
    @Column(name = "DEPARTAMENT_DESCRIPTION")
    protected String departamentDescription;

    @Column(name = "DEPARTAMENT_NUMBER_OF_PROGRAMMERS")
    protected Integer departamentNumberOfProgrammers;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "projectDepartment")
    protected List<Project> projectOfDepartment;

    public List<Project> getProjectOfDepartment() {
        return projectOfDepartment;
    }

    public void setProjectOfDepartment(List<Project> projectOfDepartment) {
        this.projectOfDepartment = projectOfDepartment;
    }

    public Integer getDepartamentNumberOfProgrammers() {
        return departamentNumberOfProgrammers;
    }

    public void setDepartamentNumberOfProgrammers(Integer departamentNumberOfProgrammers) {
        this.departamentNumberOfProgrammers = departamentNumberOfProgrammers;
    }

    public String getDepartamentDescription() {
        return departamentDescription;
    }

    public void setDepartamentDescription(String departamentDescription) {
        this.departamentDescription = departamentDescription;
    }

    public Person getDepartamentDirector() {
        return departamentDirector;
    }

    public void setDepartamentDirector(Person departamentDirector) {
        this.departamentDirector = departamentDirector;
    }

    public Person getDepartamentHrDirector() {
        return departamentHrDirector;
    }

    public void setDepartamentHrDirector(Person departamentHrDirector) {
        this.departamentHrDirector = departamentHrDirector;
    }

    public Company getCompanyName() {
        return companyName;
    }

    public void setCompanyName(Company companyName) {
        this.companyName = companyName;
    }

    public String getDepartamentRuName() {
        return departamentRuName;
    }

    public void setDepartamentRuName(String departamentRuName) {
        this.departamentRuName = departamentRuName;
    }
}