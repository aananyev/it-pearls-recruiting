package com.company.hunttech.dto.yandex;

import java.io.Serializable;

public class YandexCalendarInfoDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String displayName;
    private String path;
    private boolean isDefault;
    private boolean isClientInterviewCalendar;

    public YandexCalendarInfoDto() {
    }

    public YandexCalendarInfoDto(String id, String displayName, String path, boolean isDefault, boolean isClientInterviewCalendar) {
        this.id = id;
        this.displayName = displayName;
        this.path = path;
        this.isDefault = isDefault;
        this.isClientInterviewCalendar = isClientInterviewCalendar;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isClientInterviewCalendar() {
        return isClientInterviewCalendar;
    }

    public void setClientInterviewCalendar(boolean clientInterviewCalendar) {
        isClientInterviewCalendar = clientInterviewCalendar;
    }

    @Override
    public String toString() {
        return displayName != null ? displayName : path;
    }
}
