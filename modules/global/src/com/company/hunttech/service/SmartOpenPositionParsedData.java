package com.company.hunttech.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Структурированные данные распознанной вакансии для умного открытия/загрузки.
 */
public class SmartOpenPositionParsedData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String rawText;
    private String vacansyName;
    private String projectName;
    private String companyName;
    private String projectShortDescription;
    private String projectFullDescription;
    private String positionTypeName;
    private String gradeName;
    private String cityName;
    private Integer remoteWork = 1; // 0=Офис, 1=Удаленно, 2=Гибрид
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private BigDecimal salaryIE;
    private String salaryComment;
    private Integer workExperience = 3;
    private Integer numberPosition = 1;
    private Integer priority = 2; // Обычный
    private String shortDescription;
    private String comment;
    private String exercise;
    private String memoForInterview;
    private String interviewChecklist;
    private String searchMap;
    private String interviewPlan;
    private List<String> requiredSkills = new ArrayList<>();
    private List<String> checklist = new ArrayList<>();
    private List<String> missingFields = new ArrayList<>();

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getVacansyName() {
        return vacansyName;
    }

    public void setVacansyName(String vacansyName) {
        this.vacansyName = vacansyName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPositionTypeName() {
        return positionTypeName;
    }

    public void setPositionTypeName(String positionTypeName) {
        this.positionTypeName = positionTypeName;
    }

    public String getGradeName() {
        return gradeName;
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public Integer getRemoteWork() {
        return remoteWork;
    }

    public void setRemoteWork(Integer remoteWork) {
        this.remoteWork = remoteWork;
    }

    public BigDecimal getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(BigDecimal salaryMin) {
        this.salaryMin = salaryMin;
    }

    public BigDecimal getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(BigDecimal salaryMax) {
        this.salaryMax = salaryMax;
    }

    public Integer getWorkExperience() {
        return workExperience;
    }

    public void setWorkExperience(Integer workExperience) {
        this.workExperience = workExperience;
    }

    public Integer getNumberPosition() {
        return numberPosition;
    }

    public void setNumberPosition(Integer numberPosition) {
        this.numberPosition = numberPosition;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
    }

    public List<String> getChecklist() {
        return checklist;
    }

    public void setChecklist(List<String> checklist) {
        this.checklist = checklist != null ? checklist : new ArrayList<>();
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields != null ? missingFields : new ArrayList<>();
    }

    public String getProjectShortDescription() {
        return projectShortDescription;
    }

    public void setProjectShortDescription(String projectShortDescription) {
        this.projectShortDescription = projectShortDescription;
    }

    public String getProjectFullDescription() {
        return projectFullDescription;
    }

    public void setProjectFullDescription(String projectFullDescription) {
        this.projectFullDescription = projectFullDescription;
    }

    public BigDecimal getSalaryIE() {
        return salaryIE;
    }

    public void setSalaryIE(BigDecimal salaryIE) {
        this.salaryIE = salaryIE;
    }

    public String getSalaryComment() {
        return salaryComment;
    }

    public void setSalaryComment(String salaryComment) {
        this.salaryComment = salaryComment;
    }

    public String getExercise() {
        return exercise;
    }

    public void setExercise(String exercise) {
        this.exercise = exercise;
    }

    public String getMemoForInterview() {
        return memoForInterview;
    }

    public void setMemoForInterview(String memoForInterview) {
        this.memoForInterview = memoForInterview;
    }

    public String getInterviewChecklist() {
        return interviewChecklist;
    }

    public void setInterviewChecklist(String interviewChecklist) {
        this.interviewChecklist = interviewChecklist;
    }

    public String getSearchMap() {
        return searchMap;
    }

    public void setSearchMap(String searchMap) {
        this.searchMap = searchMap;
    }

    public String getInterviewPlan() {
        return interviewPlan;
    }

    public void setInterviewPlan(String interviewPlan) {
        this.interviewPlan = interviewPlan;
    }
}
