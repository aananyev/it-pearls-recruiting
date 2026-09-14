package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * Справочник корпоративных календарей Яндекс 360 (CalDAV).
 * Позволяет администраторам настраивать общекорпоративные календари (интервью, заказчики, общие встречи),
 * определять календарь по умолчанию и управлять доступностью в формах взаимодействий.
 */
@Table(name = "HUNTTECH_CORP_YANDEX_CAL")
@Entity(name = "hunttech_CorporateYandexCalendar")
@NamePattern("%s|name")
public class CorporateYandexCalendar extends StandardEntity {
    private static final long serialVersionUID = -518294729184719284L;

    public static final String DEFAULT_CALENDAR_BASE_URL = "https://caldav.yandex.ru";

    @NotNull
    @Column(name = "NAME", nullable = false, length = 255)
    private String name;

    @NotNull
    @Column(name = "CALENDAR_PATH", nullable = false, length = 512)
    private String calendarPath;

    @Column(name = "ACCOUNT_EMAIL", length = 255)
    private String accountEmail;

    @Column(name = "CALENDAR_BASE_URL", length = 512)
    private String calendarBaseUrl = DEFAULT_CALENDAR_BASE_URL;

    @Column(name = "IS_DEFAULT")
    private Boolean isDefault = false;

    @Column(name = "ACTIVE")
    private Boolean active = true;

    @Column(name = "DESCRIPTION", length = 1024)
    private String description;

    @Column(name = "OAUTH_TOKEN_ENCRYPTED", length = 4096)
    private String oauthTokenEncrypted;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCalendarPath() {
        return calendarPath;
    }

    public void setCalendarPath(String calendarPath) {
        this.calendarPath = calendarPath;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public String getCalendarBaseUrl() {
        return calendarBaseUrl;
    }

    public void setCalendarBaseUrl(String calendarBaseUrl) {
        this.calendarBaseUrl = calendarBaseUrl;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOauthTokenEncrypted() {
        return oauthTokenEncrypted;
    }

    public void setOauthTokenEncrypted(String oauthTokenEncrypted) {
        this.oauthTokenEncrypted = oauthTokenEncrypted;
    }
}
