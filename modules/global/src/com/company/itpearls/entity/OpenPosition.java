package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@NamePattern("%s %s|vacansyID,vacansyName")
@Table(name = "ITPEARLS_OPEN_POSITION", indexes = {
        @Index(name = "IDX_ITPEARLS_OPEN_POSITION_OPEN_CLOSE", columnList = "OPEN_CLOSE"),
        @Index(name = "IDX_ITPEARLS_OPEN_POSITION_PRIORITY", columnList = "PRIORITY"),
        @Index(name = "IDX_ITPEARLS_OPEN_POSITION_VACANSY_NAME", columnList = "VACANSY_NAME")
})
@Entity(name = "itpearls_OpenPosition")
public class OpenPosition extends StandardEntity {
    private static final long serialVersionUID = -4276280250460057561L;

    @NotNull
    @Column(name = "OPEN_CLOSE")
    protected Boolean openClose = false;

    @Column(name = "RATING")
    private Integer rating;

    @Column(name = "SIGN_DRAFT")
    private Boolean signDraft;

    @Temporal(TemporalType.DATE)
    @Column(name = "LAST_OPEN_DATE")
    private Date lastOpenDate;

    @NotNull
    @Column(name = "VACANSY_NAME", nullable = false, length = 250)
    protected String vacansyName;

    @Column(name = "VACANSY_ID", length = 16)
    private String vacansyID;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GRADE_ID")
    private Grade grade;

    @NotNull
    @Column(name = "REMOTE_WORK", nullable = false)
    protected Integer remoteWork;

    @Column(name = "REGISTRATION_FOR_WORK")
    private Integer registrationForWork;

    @Column(name = "REMOTE_COMMENT", length = 40)
    protected String remoteComment;

    @NotNull
    @Column(name = "COMMAND_CANDIDATE", nullable = false)
    protected Integer commandCandidate;

    @Column(name = "SALARY_MIN")
    protected BigDecimal salaryMin;

    @Column(name = "SALARY_MAX")
    protected BigDecimal salaryMax;

    @Column(name = "SALARY_IE")
    private BigDecimal salaryIE;

    @Column(name = "SALARY_FIX_LIMIT")
    protected Boolean salaryFixLimit;

    @Column(name = "SALARY_CANDIDATE_REQUEST")
    private Boolean salaryCandidateRequest;

    @Column(name = "SALARY_COMMENT")
    private String salaryComment;

    @Column(name = "OUTSTAFFING_COST")
    private BigDecimal outstaffingCost;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CITY_POSITION_ID")
    protected City cityPosition;

    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "openPosition")
    @Composition
    protected List<City> cities;

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

    @Column(name = "MORE10_NUMBER_POSITION")
    private Boolean more10NumberPosition;

    @Column(name = "WORK_EXPERIENCE", nullable = false)
    protected Integer workExperience;

    @Column(name = "COMMAND_EXPERIENCE")
    protected Integer commandExperience;

    @Lob
    @Column(name = "COMMENT_")
    protected String comment;

    @Lob
    @Column(name = "COMMENT_EN")
    protected String commentEn;

    @Column(name = "SHORT_DESCRIPTION", length = 250)
    @Length(message = "{msg://itpearls_OpenPosition.shortDescription.validation.Length}", max = 250)
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

    @Column(name = "PRIORITY_COMMENT")
    private String priorityComment;

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

    @Column(name = "NEED_MEMO_FOR_INTERVIEW")
    private Boolean needMemoForInterview;

    @Lob
    @Column(name = "MEMO_FOR_INTERVIEW")
    private String memoForInterview;

    @JoinTable(name = "ITPEARLS_OPEN_POSITION_LABOR_AGREEMENT_LINK",
            joinColumns = @JoinColumn(name = "OPEN_POSITION_ID"),
            inverseJoinColumns = @JoinColumn(name = "LABOR_AGREEMENT_ID"))
    @ManyToMany
    @OnDelete(DeletePolicy.CASCADE)
    @Composition
    private List<LaborAgreement> laborAgreement;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "openPosition")
    private List<OpenPositionComment> openPositionComments;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup", "open", "clear"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OWNER_ID")
    private ExtUser owner;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "openPosition")
    private List<SomeFilesOpenPosition> someFiles;

    @Temporal(TemporalType.DATE)
    @Column(name = "CLOSING_DATE")
    private Date closingDate;

    public Date getClosingDate() {
        return closingDate;
    }

    public void setClosingDate(Date closingDate) {
        this.closingDate = closingDate;
    }

    public void setOwner(ExtUser owner) {
        this.owner = owner;
    }

    public ExtUser getOwner() {
        return owner;
    }

    public List<SomeFilesOpenPosition> getSomeFiles() {
        return someFiles;
    }

    public void setSomeFiles(List<SomeFilesOpenPosition> someFiles) {
        this.someFiles = someFiles;
    }

    public List<OpenPositionComment> getOpenPositionComments() {
        return openPositionComments;
    }

    public void setOpenPositionComments(List<OpenPositionComment> openPositionComments) {
        this.openPositionComments = openPositionComments;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public BigDecimal getSalaryIE() {
        return salaryIE;
    }

    public void setSalaryIE(BigDecimal salaryIE) {
        this.salaryIE = salaryIE;
    }

    public String getVacansyID() {
        return vacansyID;
    }

    public void setVacansyID(String vacansyID) {
        this.vacansyID = vacansyID;
    }

    public Boolean getSalaryCandidateRequest() {
        return salaryCandidateRequest;
    }

    public void setSalaryCandidateRequest(Boolean salaryCandidateRequest) {
        this.salaryCandidateRequest = salaryCandidateRequest;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getPriority() {
        return priority;
    }

    public Grade getGrade() {
        return grade;
    }

    public void setGrade(Grade grade) {
        this.grade = grade;
    }

    public String getPriorityComment() {
        return priorityComment;
    }

    public void setPriorityComment(String priorityComment) {
        this.priorityComment = priorityComment;
    }

    public String getSalaryComment() {
        return salaryComment;
    }

    public void setSalaryComment(String salaryComment) {
        this.salaryComment = salaryComment;
    }

    public BigDecimal getOutstaffingCost() {
        return outstaffingCost;
    }

    public void setOutstaffingCost(BigDecimal outstaffingCost) {
        this.outstaffingCost = outstaffingCost;
    }

    public Boolean getSignDraft() {
        return signDraft;
    }

    public void setSignDraft(Boolean signDraft) {
        this.signDraft = signDraft;
    }

    public Date getLastOpenDate() {
        return lastOpenDate;
    }

    public void setLastOpenDate(Date lastOpenDate) {
        this.lastOpenDate = lastOpenDate;
    }

    public List<LaborAgreement> getLaborAgreement() {
        return laborAgreement;
    }

    public void setLaborAgreement(List<LaborAgreement> laborAgreement) {
        this.laborAgreement = laborAgreement;
    }

    public void setMore10NumberPosition(Boolean more10NumberPosition) {
        this.more10NumberPosition = more10NumberPosition;
    }

    public Boolean getMore10NumberPosition() {
        return more10NumberPosition;
    }

    public Integer getRegistrationForWork() {
        return registrationForWork;
    }

    public void setRegistrationForWork(Integer registrationForWork) {
        this.registrationForWork = registrationForWork;
    }

    public Boolean getNeedMemoForInterview() {
        return needMemoForInterview;
    }

    public void setNeedMemoForInterview(Boolean needMemoForInterview) {
        this.needMemoForInterview = needMemoForInterview;
    }


    public String getMemoForInterview() {
        return memoForInterview;
    }

    public void setMemoForInterview(String memoForInterview) {
        this.memoForInterview = memoForInterview;
    }

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

    public String getRemoteComment() {
        return remoteComment;
    }

    public void setRemoteComment(String remoteComment) {
        this.remoteComment = remoteComment;
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

    public List<City> getCities() {
        return cities;
    }

    public void setCities(List<City> cities) {
        this.cities = cities;
    }

    public String getCommentEn() {
        return commentEn;
    }

    public void setCommentEn(String commentEn) {
        this.commentEn = commentEn;
    }

    public void setSalaryFixLimit(Boolean salaryFixLimit) {
        this.salaryFixLimit = salaryFixLimit;
    }

    public Boolean getSalaryFixLimit() {
        return salaryFixLimit;
    }
}