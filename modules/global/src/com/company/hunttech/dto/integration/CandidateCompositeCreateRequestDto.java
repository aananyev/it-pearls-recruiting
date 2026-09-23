package com.company.hunttech.dto.integration;

import java.io.Serializable;
import java.util.Date;

/**
 * Комплексный запрос на создание кандидата, резюме и взаимодействия (BL-2026-026).
 */
public class CandidateCompositeCreateRequestDto implements Serializable {
    private static final long serialVersionUID = 7918237461928377L;

    private String externalId;
    private String idempotencyKey;
    private String correlationId;

    // Основные реквизиты кандидата
    private String firstName;
    private String secondName;
    private String middleName;
    private String phone;
    private String mobilePhone;
    private String email;
    private String telegramName;
    private String skypeName;
    private Date birthDate;
    private String cityId;
    private String positionId;
    private String companyId;

    // Данные резюме (опционально)
    private String cvText;
    private String cvUrl;
    private String coverLetter;

    // Данные взаимодействия / привязки к вакансии (опционально)
    private String vacancyId;
    private String interactionTypeId;
    private String interactionComment;
    private String communicationMethod;
    private Integer rating;

    public CandidateCompositeCreateRequestDto() {
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelegramName() {
        return telegramName;
    }

    public void setTelegramName(String telegramName) {
        this.telegramName = telegramName;
    }

    public String getSkypeName() {
        return skypeName;
    }

    public void setSkypeName(String skypeName) {
        this.skypeName = skypeName;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCvText() {
        return cvText;
    }

    public void setCvText(String cvText) {
        this.cvText = cvText;
    }

    public String getCvUrl() {
        return cvUrl;
    }

    public void setCvUrl(String cvUrl) {
        this.cvUrl = cvUrl;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getVacancyId() {
        return vacancyId;
    }

    public void setVacancyId(String vacancyId) {
        this.vacancyId = vacancyId;
    }

    public String getInteractionTypeId() {
        return interactionTypeId;
    }

    public void setInteractionTypeId(String interactionTypeId) {
        this.interactionTypeId = interactionTypeId;
    }

    public String getInteractionComment() {
        return interactionComment;
    }

    public void setInteractionComment(String interactionComment) {
        this.interactionComment = interactionComment;
    }

    public String getCommunicationMethod() {
        return communicationMethod;
    }

    public void setCommunicationMethod(String communicationMethod) {
        this.communicationMethod = communicationMethod;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
