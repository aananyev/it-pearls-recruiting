package com.company.hunttech.dto.yandex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class YandexMeetingResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String eventUid;
    private String calendarName;
    private String calendarPath;
    private String telemostJoinUrl;
    private String telemostConferenceId;
    private String summary;
    private Date startTime;
    private Date endTime;
    private List<String> invitedEmails = new ArrayList<>();
    private UUID iteractionListId;
    private String message;
    private String icsContent;

    public static YandexMeetingResult success(String eventUid, String calendarName, String telemostJoinUrl, String message) {
        YandexMeetingResult res = new YandexMeetingResult();
        res.setSuccess(true);
        res.setEventUid(eventUid);
        res.setCalendarName(calendarName);
        res.setTelemostJoinUrl(telemostJoinUrl);
        res.setMessage(message);
        return res;
    }

    public static YandexMeetingResult error(String message) {
        YandexMeetingResult res = new YandexMeetingResult();
        res.setSuccess(false);
        res.setMessage(message);
        return res;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getEventUid() {
        return eventUid;
    }

    public void setEventUid(String eventUid) {
        this.eventUid = eventUid;
    }

    public String getCalendarName() {
        return calendarName;
    }

    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }

    public String getCalendarPath() {
        return calendarPath;
    }

    public void setCalendarPath(String calendarPath) {
        this.calendarPath = calendarPath;
    }

    public String getTelemostJoinUrl() {
        return telemostJoinUrl;
    }

    public void setTelemostJoinUrl(String telemostJoinUrl) {
        this.telemostJoinUrl = telemostJoinUrl;
    }

    public String getTelemostConferenceId() {
        return telemostConferenceId;
    }

    public void setTelemostConferenceId(String telemostConferenceId) {
        this.telemostConferenceId = telemostConferenceId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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

    public List<String> getInvitedEmails() {
        return invitedEmails;
    }

    public void setInvitedEmails(List<String> invitedEmails) {
        this.invitedEmails = invitedEmails != null ? invitedEmails : new ArrayList<>();
    }

    public UUID getIteractionListId() {
        return iteractionListId;
    }

    public void setIteractionListId(UUID iteractionListId) {
        this.iteractionListId = iteractionListId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getIcsContent() {
        return icsContent;
    }

    public void setIcsContent(String icsContent) {
        this.icsContent = icsContent;
    }
}
