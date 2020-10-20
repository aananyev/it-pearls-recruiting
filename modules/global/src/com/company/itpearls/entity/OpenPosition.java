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
import java.math.BigDecimal;
import java.util.List;

@NamePattern("%s|vacansyName")
@Table(name = "ITPEARLS_OPEN_POSITION", indexes = {
        @Index(name = "IDX_ITPEARLS_OPEN_POSITION_VACANCY_NAME", columnList = "VACANSY_NAME")
})
@Entity(name = "itpearls_OpenPosition")
public class OpenPosition extends StandardEntity {
    private static final long serialVersionUID = -4276280250460057561L;

    @NotNull
    @Column(name = "OPEN_CLOSE")
    protected Boolean openClose = false;

    @NotNull
    @Column(name = "VACANSY_NAME", nullable = false, length = 150)
    protected String vacansyName;

    @NotNull
    @Column(name = "REMOTE_WORK", nullable = false)
    protected Integer remoteWork;

    @NotNull
    @Column(name = "COMMAND_CANDIDATE")
    protected Integer commandCandidate;

    @Column(name = "SALARY_MIN")
    protected BigDecimal salaryMin;

    @Column(name = "SALARY_MAX")
    protected BigDecimal salaryMax;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CITY_POSITION_ID")
    protected City cityPosition;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POSITION_TYPE_ID")
    protected Position positionType;

    @NotNull
    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PROJECT_NAME_ID")
    protected Project projectName;

    @Column(name = "NUMBER_POSITION")
    protected Integer numberPosition;

    @Column(name = "WORK_EXPERIENCE")
    protected Integer workExperience;


    @Column(name = "COMMAND_EXPERIENCE")
    protected Integer commandExperience;

    @Lob
    @Column(name = "COMMENT_")
    protected String comment;

    @Column(name = "SHORT_DESCRIPTION", length = 160)
    protected String shortDescription;

    @Lob
    @Column(name = "TEMPLATE_LETTER")
    protected String templateLetter;

    @Column(name = "NEED_LETTER")
    protected Boolean needLetter;

    @Lob
    @Column(name = "EXERCISE")
    protected String exercise;

    @Column(name = "NEED_EXERCISE")
    protected Boolean needExercise;

    @Column(name = "PRIORITY")
    protected Integer priority;

    @Composition
    @OnDelete(DeletePolicy.DENY)
    @OneToMany(mappedBy = "openPosition")
    protected List<SkillTree> skillsList;

    @JoinTable(name = "ITPEARLS_OPEN_POSITION_RECRUTIES_TASKS_LINK", joinColumns = @JoinColumn(name = "OPEN_POSITION_ID"), inverseJoinColumns = @JoinColumn(name = "RECRUTIES_TASKS_ID"))
    @ManyToMany
    @OnDelete(DeletePolicy.DENY)
    protected List<RecrutiesTasks> candidates;

    @Column(name = "PAYMENTS_TYPE")
    protected Integer paymentsType;

    @Column(name = "TYPE_COMPANY_COMISSION")
    protected Integer typeCompanyComission;

    @Column(name = "TYPE_SALARY_OF_RESEARCHER")
    protected Integer typeSalaryOfResearcher;

    @Column(name = "TYPE_SALARY_OF_RECRUTIER")
    protected Integer typeSalaryOfRecrutier;

    @Column(name = "USE_TAX_NDFL")
    protected Boolean useTaxNDFL;

    @NotNull
    @Column(name = "INTERNAL_PROJECT")
    protected Boolean internalProject = false;

    @Column(name = "PERCENT_COMISSION_OF_COMPANY", length = 5)
    protected String percentComissionOfCompany;

    @Column(name = "PERCENT_SALARY_OF_RESEARCHER", length = 5)
    protected String percentSalaryOfResearcher;

    @Column(name = "PERCENT_SALARY_OF_RECRUTIER", length = 5)
    protected String percentSalaryOfRecrutier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_OPEN_POSITION_ID")
    protected OpenPosition parentOpenPosition;

    public Boolean getInternalProject() {
        return internalProject;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setInternalProject(Boolean internalProject) {
        this.internalProject = internalProject;
    }

    public Integer getRemoteWork() {
        return remoteWork;
    }

    public void setRemoteWork(Integer remoteWork) {
        this.remoteWork = remoteWork;
    }

    public Boolean getUseTaxNDFL() {
        return useTaxNDFL;
    }

    public void setUseTaxNDFL(Boolean useTaxNDFL) {
        this.useTaxNDFL = useTaxNDFL;
    }

    public String getPercentSalaryOfRecrutier() {
        return percentSalaryOfRecrutier;
    }

    public void setPercentSalaryOfRecrutier(String percentSalaryOfRecrutier) {
        this.percentSalaryOfRecrutier = percentSalaryOfRecrutier;
    }

    public String getPercentSalaryOfResearcher() {
        return percentSalaryOfResearcher;
    }

    public void setPercentSalaryOfResearcher(String percentSalaryOfResearcher) {
        this.percentSalaryOfResearcher = percentSalaryOfResearcher;
    }

    public String getPercentComissionOfCompany() {
        return percentComissionOfCompany;
    }

    public void setPercentComissionOfCompany(String percentComissionOfCompany) {
        this.percentComissionOfCompany = percentComissionOfCompany;
    }

    public void setTypeSalaryOfRecrutier(Integer typeSalaryOfRecrutier) {
        this.typeSalaryOfRecrutier = typeSalaryOfRecrutier;
    }

    public Integer getTypeSalaryOfRecrutier() {
        return typeSalaryOfRecrutier;
    }

    public Integer getTypeSalaryOfResearcher() {
        return typeSalaryOfResearcher;
    }

    public void setTypeSalaryOfResearcher(Integer typeSalaryOfResearcher) {
        this.typeSalaryOfResearcher = typeSalaryOfResearcher;
    }

    public Integer getTypeCompanyComission() {
        return typeCompanyComission;
    }

    public void setTypeCompanyComission(Integer typeCompanyComission) {
        this.typeCompanyComission = typeCompanyComission;
    }

    public Integer getPaymentsType() {
        return paymentsType;
    }

    public void setPaymentsType(Integer paymentsType) {
        this.paymentsType = paymentsType;
    }

    public void setCandidates(List<RecrutiesTasks> candidates) {
        this.candidates = candidates;
    }

    public List<RecrutiesTasks> getCandidates() {
        return candidates;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public BigDecimal getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(BigDecimal salaryMax) {
        this.salaryMax = salaryMax;
    }

    public BigDecimal getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(BigDecimal salaryMin) {
        this.salaryMin = salaryMin;
    }

    public Boolean getOpenClose() {
        return openClose;
    }

    public void setOpenClose(Boolean openClose) {
        this.openClose = openClose;
    }

    public City getCityPosition() {
        return cityPosition;
    }

    public void setCityPosition(City cityPosition) {
        this.cityPosition = cityPosition;
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

    public Integer getNumberPosition() {
        return numberPosition;
    }

    public void setNumberPosition(Integer numberPosition) {
        this.numberPosition = numberPosition;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTemplateLetter() {
        return templateLetter;
    }

    public void setTemplateLetter(String templateLetter) {
        this.templateLetter = templateLetter;
    }

    public Project getProjectName() {
        return projectName;
    }

    public String getExercise() {
        return exercise;
    }

    public void setExercise(String exercise) {
        this.exercise = exercise;
    }

    public Boolean getNeedExercise() {
        return needExercise;
    }

    public void setNeedExercise(Boolean needExercise) {
        this.needExercise = needExercise;
    }

    public void setProjectName(Project projectName) {
        this.projectName = projectName;
    }

    public void setWorkExperience(Integer workExperience) {
        this.workExperience = workExperience;
    }

    public Integer getWorkExperience() {
        return workExperience;
    }

    public void setCommandExperience(Integer commandExperience) {
        this.commandExperience = commandExperience;
    }

    public Integer getCommandExperience() {
        return commandExperience;
    }

    public Boolean getNeedLetter() {
        return needLetter;
    }

    public void setNeedLetter(Boolean needLetter) {
        this.needLetter = needLetter;
    }

    public OpenPosition getParentOpenPosition() {
        return parentOpenPosition;
    }

    public void setParentOpenPosition(OpenPosition parentOpenPosition) {
        this.parentOpenPosition = parentOpenPosition;
    }

    public void setCommandCandidate(Integer commandCandidate) {
        this.commandCandidate = commandCandidate;
    }

    public Integer getCommandCandidate() {
        return commandCandidate;
    }
}