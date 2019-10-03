package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@NamePattern("%s|projectName")
@Table(name = "ITPEARLS_PROJECT")
@Entity(name = "itpearls_Project")
public class Project extends StandardEntity {
    private static final long serialVersionUID = 8105712812181375543L;

    @Column(name = "PROJECT_NAME", length = 80)
    protected String projectName;

    @Temporal(TemporalType.DATE)
    @Column(name = "START_PROJECT_DATE")
    protected Date startProjectDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "END_PROJECT_DATE")
    protected Date endProjectDate;

    @NotNull
    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PROJECT_DEPARTMENT_ID")
    protected CompanyDepartament projectDepartment;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_OWNER_ID")
    protected Person projectOwner;

    public Person getProjectOwner() {
        return projectOwner;
    }

    public void setProjectOwner(Person projectOwner) {
        this.projectOwner = projectOwner;
    }

    public CompanyDepartament getProjectDepartment() {
        return projectDepartment;
    }

    public void setProjectDepartment(CompanyDepartament projectDepartment) {
        this.projectDepartment = projectDepartment;
    }

    public Date getEndProjectDate() {
        return endProjectDate;
    }

    public void setEndProjectDate(Date endProjectDate) {
        this.endProjectDate = endProjectDate;
    }

    public Date getStartProjectDate() {
        return startProjectDate;
    }

    public void setStartProjectDate(Date startProjectDate) {
        this.startProjectDate = startProjectDate;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}