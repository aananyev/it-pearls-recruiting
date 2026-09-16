package com.company.hunttech.dto.action;

import java.io.Serializable;
import java.util.Date;

public class InterviewSchedulingIntent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String rawMessage;
    private String candidateQuery;
    private String vacancyQuery;
    private String projectQuery;
    private String projectOwnerQuery;
    private String interactionTypeQuery;
    private String rawDateTime;
    private Date targetDateTime;
    private String timeZone = "Europe/Moscow";
    private boolean online = true;
    private boolean intentDetected = false;

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getCandidateQuery() {
        return candidateQuery;
    }

    public void setCandidateQuery(String candidateQuery) {
        this.candidateQuery = candidateQuery;
    }

    public String getVacancyQuery() {
        return vacancyQuery;
    }

    public void setVacancyQuery(String vacancyQuery) {
        this.vacancyQuery = vacancyQuery;
    }

    public String getProjectQuery() {
        return projectQuery;
    }

    public void setProjectQuery(String projectQuery) {
        this.projectQuery = projectQuery;
    }

    public String getProjectOwnerQuery() {
        return projectOwnerQuery;
    }

    public void setProjectOwnerQuery(String projectOwnerQuery) {
        this.projectOwnerQuery = projectOwnerQuery;
    }

    public String getInteractionTypeQuery() {
        return interactionTypeQuery;
    }

    public void setInteractionTypeQuery(String interactionTypeQuery) {
        this.interactionTypeQuery = interactionTypeQuery;
    }

    public String getRawDateTime() {
        return rawDateTime;
    }

    public void setRawDateTime(String rawDateTime) {
        this.rawDateTime = rawDateTime;
    }

    public Date getTargetDateTime() {
        return targetDateTime;
    }

    public void setTargetDateTime(Date targetDateTime) {
        this.targetDateTime = targetDateTime;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isIntentDetected() {
        return intentDetected;
    }

    public void setIntentDetected(boolean intentDetected) {
        this.intentDetected = intentDetected;
    }
}
