package com.company.hunttech.service.dto;

import java.io.Serializable;
import java.util.UUID;

public class TakeIntoWorkResult implements Serializable {
    private static final long serialVersionUID = 129481928419283L;

    private boolean success;
    private boolean alreadyInWork;
    private UUID interactionId;
    private String message;
    private String candidateFullName;
    private String vacancyTitle;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isAlreadyInWork() {
        return alreadyInWork;
    }

    public void setAlreadyInWork(boolean alreadyInWork) {
        this.alreadyInWork = alreadyInWork;
    }

    public UUID getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(UUID interactionId) {
        this.interactionId = interactionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCandidateFullName() {
        return candidateFullName;
    }

    public void setCandidateFullName(String candidateFullName) {
        this.candidateFullName = candidateFullName;
    }

    public String getVacancyTitle() {
        return vacancyTitle;
    }

    public void setVacancyTitle(String vacancyTitle) {
        this.vacancyTitle = vacancyTitle;
    }
}
