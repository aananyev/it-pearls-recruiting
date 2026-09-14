package com.company.hunttech.dto.yandex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class YandexMeetingRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private YandexCalendarType calendarType = YandexCalendarType.PERSONAL;
    private String customCalendarPath;
    private String title;
    private String description;
    private Date startTime;
    private Date endTime;
    private String timeZone;

    private String eventUid;
    private UUID candidateId;
    private String candidateName;
    private String candidateEmail;

    private List<String> attendeeEmails = new ArrayList<>();
    private List<UUID> hunttechUserIds = new ArrayList<>();

    private boolean createTelemostMeeting = true;
    private boolean telemostAutoRecord = true;
    private boolean telemostAiSummary = true;

    private UUID openPositionId;

    public YandexCalendarType getCalendarType() {
        return calendarType;
    }

    public void setCalendarType(YandexCalendarType calendarType) {
        this.calendarType = calendarType;
    }

    public String getCustomCalendarPath() {
        return customCalendarPath;
    }

    public void setCustomCalendarPath(String customCalendarPath) {
        this.customCalendarPath = customCalendarPath;
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

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public List<String> getAttendeeEmails() {
        return attendeeEmails;
    }

    public void setAttendeeEmails(List<String> attendeeEmails) {
        this.attendeeEmails = attendeeEmails != null ? attendeeEmails : new ArrayList<>();
    }

    public List<UUID> getHunttechUserIds() {
        return hunttechUserIds;
    }

    public void setHunttechUserIds(List<UUID> hunttechUserIds) {
        this.hunttechUserIds = hunttechUserIds != null ? hunttechUserIds : new ArrayList<>();
    }

    public boolean isCreateTelemostMeeting() {
        return createTelemostMeeting;
    }

    public void setCreateTelemostMeeting(boolean createTelemostMeeting) {
        this.createTelemostMeeting = createTelemostMeeting;
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

    public UUID getOpenPositionId() {
        return openPositionId;
    }

    public void setOpenPositionId(UUID openPositionId) {
        this.openPositionId = openPositionId;
    }

    public String getEventUid() {
        return eventUid;
    }

    public void setEventUid(String eventUid) {
        this.eventUid = eventUid;
    }
}
