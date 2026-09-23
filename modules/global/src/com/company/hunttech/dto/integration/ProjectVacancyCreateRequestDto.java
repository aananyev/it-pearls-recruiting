package com.company.hunttech.dto.integration;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Запрос транзакционного создания проекта и вакансии из внешней системы (BL-2026-025).
 */
public class ProjectVacancyCreateRequestDto implements Serializable {
    private static final long serialVersionUID = 4192837461928371L;

    private String externalId;
    private String idempotencyKey;
    private String correlationId;

    // Параметры проекта
    private String existingProjectId;
    private String projectName;
    private String projectDescription;
    private String companyId;

    // Параметры вакансии
    private String vacancyName;
    private String shortDescription;
    private String comment;
    private Integer remoteWork = 1; // 1 = Удаленка по умолчанию
    private Integer commandCandidate = 1;
    private Integer workExperience = 1;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String gradeId;
    private String cityId;
    private String positionTypeId;

    public ProjectVacancyCreateRequestDto() {
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getExistingProjectId() {
        return existingProjectId;
    }

    public void setExistingProjectId(String existingProjectId) {
        this.existingProjectId = existingProjectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getVacancyName() {
        return vacancyName;
    }

    public void setVacancyName(String vacancyName) {
        this.vacancyName = vacancyName;
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

    public Integer getRemoteWork() {
        return remoteWork;
    }

    public void setRemoteWork(Integer remoteWork) {
        this.remoteWork = remoteWork;
    }

    public Integer getCommandCandidate() {
        return commandCandidate;
    }

    public void setCommandCandidate(Integer commandCandidate) {
        this.commandCandidate = commandCandidate;
    }

    public Integer getWorkExperience() {
        return workExperience;
    }

    public void setWorkExperience(Integer workExperience) {
        this.workExperience = workExperience;
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

    public String getGradeId() {
        return gradeId;
    }

    public void setGradeId(String gradeId) {
        this.gradeId = gradeId;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getPositionTypeId() {
        return positionTypeId;
    }

    public void setPositionTypeId(String positionTypeId) {
        this.positionTypeId = positionTypeId;
    }
}
