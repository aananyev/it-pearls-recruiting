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

@NamePattern("%s|vacansyName")
@Table(name = "ITPEARLS_OPEN_POSITION")
@Entity(name = "itpearls_OpenPosition")
public class OpenPosition extends StandardEntity {
    private static final long serialVersionUID = -4276280250460057561L;

    @Column(name = "OPEN_FLAG")
    protected Boolean openFlag;

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

    @Column(name = "NUMBER_POSITION")
    protected Integer numberPosition;

    @Lob
    @Column(name = "COMMENT_")
    protected String comment;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "openPosition")
    protected List<SkillTree> skillsList;

    public Boolean getOpenFlag() {
        return openFlag;
    }

    public void setOpenFlag(Boolean openFlag) {
        this.openFlag = openFlag;
    }

    public List<SkillTree> getSkillsList() {
        return skillsList;
    }

    public void setSkillsList(List<SkillTree> skillsList) {
        this.skillsList = skillsList;
    }

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