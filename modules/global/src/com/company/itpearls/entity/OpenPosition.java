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

@NamePattern("%s %s|vacansyName,companyDepartament")
@Table(name = "ITPEARLS_OPEN_POSITION", indexes = {
        @Index(name = "IDX_ITPEARLS_OPEN_POSITION_VACANCY_NAME", columnList = "VACANSY_NAME"),
        @Index(name = "IDX_ITPEARLS_OPEN_POSITION_COMPANY_NAME", columnList = "COMPANY_NAME_ID")
})
@Entity(name = "itpearls_OpenPosition")
public class OpenPosition extends StandardEntity {
    private static final long serialVersionUID = -4276280250460057561L;

    @Column(name = "OPEN_CLOSE")
    protected Boolean openClose;

    @NotNull
    @Column(name = "VACANSY_NAME", nullable = false, length = 80)
    protected String vacansyName;

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

    @NotNull
    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COMPANY_NAME_ID")
    protected Company companyName;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_DEPARTAMENT_ID")
    protected CompanyDepartament companyDepartament;

    @Column(name = "NUMBER_POSITION")
    protected Integer numberPosition;

    @Lob
    @Column(name = "COMMENT_")
    protected String comment;

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

    @Column(name = "PERCENT_COMISSION_OF_COMPANY", length = 5)
    protected String percentComissionOfCompany;

    @Column(name = "PERCENT_SALARY_OF_RESEARCHER", length = 5)
    protected String percentSalaryOfResearcher;

    @Column(name = "PERCENT_SALARY_OF_RECRUTIER", length = 5)
    protected String percentSalaryOfRecrutier;

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