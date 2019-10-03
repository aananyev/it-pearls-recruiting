package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@NamePattern("%s|projectName")
@Table(name = "ITPEARLS_OPEN_POSITION")
@Entity(name = "itpearls_OpenPosition")
public class OpenPosition extends StandardEntity {
    private static final long serialVersionUID = -4276280250460057561L;

    @NotNull
    @Column(name = "VACANSY_NAME", nullable = false, length = 80)
    protected String vacansyName;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POSITION_TYPE_ID")
    protected Position positionType;

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

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_DEPARTAMENT_ID")
    protected CompanyDepartament companyDepartament;

    @NotNull
    @Column(name = "NUMBER_POSITION", nullable = false)
    protected Integer numberPosition;

    @Lob
    @Column(name = "COMMENT_")
    protected String comment;

    public Position getPositionType() {
        return positionType;
    }

    public void setPositionType(Position positionType) {
        this.positionType = positionType;
    }

    public String getVacansyName() {
        return vacansyName;
    }

    public void setVacansyName(String vacansyName) {
        this.vacansyName = vacansyName;
    }

    public CompanyDepartament getCompanyDepartament() {
        return companyDepartament;
    }

    public void setCompanyDepartament(CompanyDepartament companyDepartament) {
        this.companyDepartament = companyDepartament;
    }

    public Integer getNumberPosition() {
        return numberPosition;
    }

    public void setNumberPosition(Integer numberPosition) {
        this.numberPosition = numberPosition;
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