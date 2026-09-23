package com.company.hunttech.dto.integration;

import java.io.Serializable;
import java.util.Date;

/**
 * Запрос на создание взаимодействия с кандидатом (BL-2026-028).
 */
public class InteractionCreateRequestDto implements Serializable {
    private static final long serialVersionUID = 4918237461928375L;

    private String externalId;
    private String idempotencyKey;
    private String correlationId;

    private String candidateId;
    private String vacancyId;
    private String interactionTypeId;
    private Date interactionDate;
    private String comment;
    private String communicationMethod;
    private Integer rating;
    private String recruiterId;

    public InteractionCreateRequestDto() {
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

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
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

    public Date getInteractionDate() {
        return interactionDate;
    }

    public void setInteractionDate(Date interactionDate) {
        this.interactionDate = interactionDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    public String getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(String recruiterId) {
        this.recruiterId = recruiterId;
    }
}
