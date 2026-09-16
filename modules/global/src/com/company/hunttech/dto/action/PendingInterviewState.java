package com.company.hunttech.dto.action;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PendingInterviewState implements Serializable {
    private static final long serialVersionUID = 1L;

    private String step; // NEED_CANDIDATE, NEED_VACANCY, NEED_INTERACTION_TYPE, CONFIRM_SCHEDULE
    private UUID candidateId;
    private String candidateFio;
    private String candidateEmail;
    private List<UUID> candidateOptions;

    private UUID vacancyId;
    private String vacancyName;
    private String projectName;
    private List<UUID> vacancyOptions;

    private UUID interactionTypeId;
    private String interactionTypeName;
    private List<UUID> interactionTypeOptions;

    private Date targetDateTime;
    private String timeZone = "Europe/Moscow";
    private boolean online = true;
    private String rawQuery;

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateFio() {
        return candidateFio;
    }

    public void setCandidateFio(String candidateFio) {
        this.candidateFio = candidateFio;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public List<UUID> getCandidateOptions() {
        return candidateOptions;
    }

    public void setCandidateOptions(List<UUID> candidateOptions) {
        this.candidateOptions = candidateOptions;
    }

    public UUID getVacancyId() {
        return vacancyId;
    }

    public void setVacancyId(UUID vacancyId) {
        this.vacancyId = vacancyId;
    }

    public String getVacancyName() {
        return vacancyName;
    }

    public void setVacancyName(String vacancyName) {
        this.vacancyName = vacancyName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public List<UUID> getVacancyOptions() {
        return vacancyOptions;
    }

    public void setVacancyOptions(List<UUID> vacancyOptions) {
        this.vacancyOptions = vacancyOptions;
    }

    public UUID getInteractionTypeId() {
        return interactionTypeId;
    }

    public void setInteractionTypeId(UUID interactionTypeId) {
        this.interactionTypeId = interactionTypeId;
    }

    public String getInteractionTypeName() {
        return interactionTypeName;
    }

    public void setInteractionTypeName(String interactionTypeName) {
        this.interactionTypeName = interactionTypeName;
    }

    public List<UUID> getInteractionTypeOptions() {
        return interactionTypeOptions;
    }

    public void setInteractionTypeOptions(List<UUID> interactionTypeOptions) {
        this.interactionTypeOptions = interactionTypeOptions;
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

    public String getRawQuery() {
        return rawQuery;
    }

    public void setRawQuery(String rawQuery) {
        this.rawQuery = rawQuery;
    }
}
