package com.company.hunttech.dto.action;

import java.io.Serializable;
import java.util.UUID;

public class InterviewSchedulingResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private boolean pendingUserClarification;
    private String pendingStep; // NEED_CANDIDATE, NEED_VACANCY, NEED_INTERACTION_TYPE, CONFIRM_SCHEDULE, NONE
    private String message;
    private UUID iteractionListId;
    private String telemostJoinUrl;
    private String eventUid;
    private String calendarName;

    public static InterviewSchedulingResult success(String message, UUID iteractionListId, String telemostJoinUrl, String eventUid, String calendarName) {
        InterviewSchedulingResult res = new InterviewSchedulingResult();
        res.setSuccess(true);
        res.setMessage(message);
        res.setIteractionListId(iteractionListId);
        res.setTelemostJoinUrl(telemostJoinUrl);
        res.setEventUid(eventUid);
        res.setCalendarName(calendarName);
        res.setPendingStep("NONE");
        return res;
    }

    public static InterviewSchedulingResult pending(String pendingStep, String message) {
        InterviewSchedulingResult res = new InterviewSchedulingResult();
        res.setSuccess(false);
        res.setPendingUserClarification(true);
        res.setPendingStep(pendingStep);
        res.setMessage(message);
        return res;
    }

    public static InterviewSchedulingResult error(String message) {
        InterviewSchedulingResult res = new InterviewSchedulingResult();
        res.setSuccess(false);
        res.setMessage(message);
        res.setPendingStep("NONE");
        return res;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isPendingUserClarification() {
        return pendingUserClarification;
    }

    public void setPendingUserClarification(boolean pendingUserClarification) {
        this.pendingUserClarification = pendingUserClarification;
    }

    public String getPendingStep() {
        return pendingStep;
    }

    public void setPendingStep(String pendingStep) {
        this.pendingStep = pendingStep;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UUID getIteractionListId() {
        return iteractionListId;
    }

    public void setIteractionListId(UUID iteractionListId) {
        this.iteractionListId = iteractionListId;
    }

    public String getTelemostJoinUrl() {
        return telemostJoinUrl;
    }

    public void setTelemostJoinUrl(String telemostJoinUrl) {
        this.telemostJoinUrl = telemostJoinUrl;
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
}
