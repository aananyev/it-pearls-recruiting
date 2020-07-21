package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@NamePattern("%s|projectName")
@Table(name = "ITPEARLS_PROJECT", indexes = {
        @Index(name = "IDX_ITPEARLS_PROJECT", columnList = "PROJECT_NAME")
})
@Entity(name = "itpearls_Project")
public class Project extends StandardEntity {
    private static final long serialVersionUID = 8105712812181375543L;

    @Column(name = "PROJECT_NAME", length = 80)
    protected String projectName;

    @Column(name = "PROJECT_IS_CLOSED")
    protected Boolean projectIsClosed = false;

    @Temporal(TemporalType.DATE)
    @Column(name = "START_PROJECT_DATE")
    protected Date startProjectDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "END_PROJECT_DATE")
    protected Date endProjectDate;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_DEPARTMENT_ID")
    protected CompanyDepartament projectDepartment;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_OWNER_ID")
    protected Person projectOwner;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "projectName")
    protected List<OpenPosition> openPosition;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "project")
    protected List<IteractionList> jobCandidate;

    @Lob
    @Column(name = "PROJECT_DESCRIPTION")
    protected String projectDescription;

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public List<IteractionList> getJobCandidate() {
        return jobCandidate;
    }

    public Boolean getProjectIsClosed() {
        return projectIsClosed;
    }

    public void setProjectIsClosed(Boolean projectIsClosed) {
        this.projectIsClosed = projectIsClosed;
    }

    public void setJobCandidate(List<IteractionList> jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public List<OpenPosition> getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(List<OpenPosition> openPosition) {
        this.openPosition = openPosition;
    }

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