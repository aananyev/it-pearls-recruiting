package com.company.hunttech.dto.yandex;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * DTO события Яндекс-Календаря, полученного через CalDAV (RFC 4791 / RFC 5545).
 */
public class YandexCalendarEventDto implements Serializable, Comparable<YandexCalendarEventDto> {
    private static final long serialVersionUID = 1L;

    private String uid;
    private String summary;
    private String description;
    private Date startTime;
    private Date endTime;
    private boolean allDay;
    private String timeZone = "Europe/Saratov";
    private String calendarName;
    private String calendarPath;
    private String location;
    private String telemostUrl;
    private String status = "CONFIRMED";
    private List<String> attendees = new ArrayList<>();
    private String organizer;
    private String rawIcs;

    public YandexCalendarEventDto() {
    }

    public YandexCalendarEventDto(String uid, String summary, Date startTime, Date endTime) {
        this.uid = uid;
        this.summary = summary;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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

    public boolean isAllDay() {
        return allDay;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTelemostUrl() {
        return telemostUrl;
    }

    public void setTelemostUrl(String telemostUrl) {
        this.telemostUrl = telemostUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getAttendees() {
        return attendees;
    }

    public void setAttendees(List<String> attendees) {
        this.attendees = attendees != null ? attendees : new ArrayList<>();
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getRawIcs() {
        return rawIcs;
    }

    public void setRawIcs(String rawIcs) {
        this.rawIcs = rawIcs;
    }

    public String getFormattedTimeRange(TimeZone tz) {
        if (allDay) {
            return "Весь день";
        }
        if (startTime == null) {
            return "Время не указано";
        }
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm");
        if (tz != null) {
            tf.setTimeZone(tz);
        }
        String startStr = tf.format(startTime);
        if (endTime != null) {
            String endStr = tf.format(endTime);
            return startStr + " – " + endStr;
        }
        return startStr;
    }

    public String getFormattedDate(TimeZone tz) {
        if (startTime == null) {
            return "";
        }
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy (EEEE)", new Locale("ru", "RU"));
        if (tz != null) {
            df.setTimeZone(tz);
        }
        return df.format(startTime);
    }

    @Override
    public int compareTo(YandexCalendarEventDto other) {
        if (this.startTime == null && other.startTime == null) return 0;
        if (this.startTime == null) return 1;
        if (other.startTime == null) return -1;
        return this.startTime.compareTo(other.startTime);
    }

    @Override
    public String toString() {
        return "YandexCalendarEventDto{" +
                "summary='" + summary + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", calendarName='" + calendarName + '\'' +
                ", telemostUrl='" + telemostUrl + '\'' +
                '}';
    }
}
