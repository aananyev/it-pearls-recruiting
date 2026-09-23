package com.company.hunttech.dto.integration;

import java.io.Serializable;

/**
 * Запрос на создание резюме кандидата CandidateCV (BL-2026-028).
 */
public class CandidateCvCreateRequestDto implements Serializable {
    private static final long serialVersionUID = 8192304918237461L;

    private String externalId;
    private String idempotencyKey;
    private String correlationId;

    private String candidateId;
    private String vacancyId;
    private String positionId;
    private String textCv;
    private String resumeUrl;
    private String coverLetter;

    public CandidateCvCreateRequestDto() {
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

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public String getTextCv() {
        return textCv;
    }

    public void setTextCv(String textCv) {
        this.textCv = textCv;
    }

    public String getResumeUrl() {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl) {
        this.resumeUrl = resumeUrl;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }
}
