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

    private List<TimeSlot> timeSlots = new ArrayList<>();
    private boolean checkDuplicates = false;

    public static class TimeSlot implements Serializable {
        private static final long serialVersionUID = 1L;
        private Date startTime;
        private Date endTime;
        private String dayLabel;

        public TimeSlot() {
        }

        public TimeSlot(Date startTime, Date endTime, String dayLabel) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.dayLabel = dayLabel;
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

        public String getDayLabel() {
            return dayLabel;
        }

        public void setDayLabel(String dayLabel) {
            this.dayLabel = dayLabel;
        }
    }

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
        if (startTime != null) {
            return startTime;
        }
        if (timeSlots != null) {
            for (TimeSlot slot : timeSlots) {
                if (slot != null && slot.getStartTime() != null) {
                    return slot.getStartTime();
                }
            }
        }
        return null;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        if (endTime != null) {
            return endTime;
        }
        if (timeSlots != null) {
            for (TimeSlot slot : timeSlots) {
                if (slot != null && slot.getEndTime() != null) {
                    return slot.getEndTime();
                }
            }
        }
        return null;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public List<TimeSlot> getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(List<TimeSlot> timeSlots) {
        this.timeSlots = timeSlots != null ? timeSlots : new ArrayList<>();
    }

    public void addTimeSlot(Date start, Date end, String dayLabel) {
        if (this.timeSlots == null) {
            this.timeSlots = new ArrayList<>();
        }
        this.timeSlots.add(new TimeSlot(start, end, dayLabel));
    }

    public boolean isCheckDuplicates() {
        return checkDuplicates;
    }

    public void setCheckDuplicates(boolean checkDuplicates) {
        this.checkDuplicates = checkDuplicates;
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
