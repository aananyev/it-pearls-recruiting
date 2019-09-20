package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_OPEN_POSITION")
@Entity(name = "itpearls_OpenPosition")
public class OpenPosition extends StandardEntity {
    private static final long serialVersionUID = -4276280250460057561L;

    @NotNull
    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PROJECT_NAME_ID")
    protected Project projectName;

    @NotNull
    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COMPANY_NAME_ID")
    protected Company companyName;

    @NotNull
    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PROJECT_ID")
    protected Project project;

    @NotNull
    @Column(name = "NUMBER_POSITION", nullable = false)
    protected Integer numberPosition;

    @Lob
    @Column(name = "COMMENT_")
    protected String comment;

    public Integer getNumberPosition() {
        return numberPosition;
    }

    public void setNumberPosition(Integer numberPosition) {
        this.numberPosition = numberPosition;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Company getCompanyName() {
        return companyName;
    }

    public void setCompanyName(Company companyName) {
        this.companyName = companyName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Project getProjectName() {
        return projectName;
    }

    public void setProjectName(Project projectName) {
        this.projectName = projectName;
    }
}