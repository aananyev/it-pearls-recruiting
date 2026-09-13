package com.company.hunttech.dto.yandex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AiMeetingParseResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean intentDetected;
    private YandexCalendarType calendarType = YandexCalendarType.PERSONAL;
    private String calendarName;
    private String title;
    private String description;
    private Date startTime;
    private Date endTime;
    private String timeZone = "Europe/Saratov";

    private UUID candidateId;
    private String candidateFio;
    private String candidateEmail;

    private boolean telemostRequired = true;
    private boolean telemostAutoRecord = true;
    private boolean telemostAiSummary = true;

    private List<String> attendeeEmails = new ArrayList<>();
    private String rawUserMessage;
    private String explanation;

    public boolean isIntentDetected() {
        return intentDetected;
    }

    public void setIntentDetected(boolean intentDetected) {
        this.intentDetected = intentDetected;
    }

    public YandexCalendarType getCalendarType() {
        return calendarType;
    }

    public void setCalendarType(YandexCalendarType calendarType) {
        this.calendarType = calendarType;
    }

    public String getCalendarName() {
        return calendarName;
    }

    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
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

    public boolean isTelemostRequired() {
        return telemostRequired;
    }

    public void setTelemostRequired(boolean telemostRequired) {
        this.telemostRequired = telemostRequired;
    }

    public boolean isTelemostAutoRecord() {
        return telemostAutoRecord;
    }

    public void setTelemostAutoRecord(boolean telemostAutoRecord) {
        this.telemostAutoRecord = telemostAutoRecord;
    }

    public boolean isTelemostAiSummary() {
        return telemostAiSummary;
    }

    public void setTelemostAiSummary(boolean telemostAiSummary) {
        this.telemostAiSummary = telemostAiSummary;
    }

    public List<String> getAttendeeEmails() {
        return attendeeEmails;
    }

    public void setAttendeeEmails(List<String> attendeeEmails) {
        this.attendeeEmails = attendeeEmails != null ? attendeeEmails : new ArrayList<>();
    }

    public String getRawUserMessage() {
        return rawUserMessage;
    }

    public void setRawUserMessage(String rawUserMessage) {
        this.rawUserMessage = rawUserMessage;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
