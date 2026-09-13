package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Персональная конфигурация интеграции с сервисами Yandex 360:
 * Календари (CalDAV), Видеоконференции (Телемост), Корпоративная Wiki и почта.
 * Секреты (OAuth токены) сохраняются исключительно в зашифрованном виде через AiSecretService.
 */
@Table(name = "HUNTTECH_USER_YANDEX_CONFIG", indexes = {
        @Index(name = "IDX_HUNTTECH_USER_YANDEX_CONFIG_USER", columnList = "USER_ID", unique = true)
})
@Entity(name = "hunttech_UserYandexConfiguration")
@NamePattern("%s (%s)|accountEmail,user")
public class UserYandexConfiguration extends StandardEntity {
    private static final long serialVersionUID = 7192834019283746192L;

    public static final String DEFAULT_CALENDAR_BASE_URL = "https://caldav.yandex.ru";
    public static final String DEFAULT_CLIENT_CALENDAR_NAME = "Hunttech у заказчика";
    public static final String DEFAULT_TIME_ZONE = "Europe/Saratov";
    public static final String DEFAULT_TELEMOST_BASE_URL = "https://cloud-api.yandex.net/v1/telemost-api";
    public static final String DEFAULT_WIKI_BASE_URL = "https://wiki.yandex.ru";
    public static final String DEFAULT_WIKI_API_URL = "https://api.wiki.yandex.net/v1";
    public static final String DEFAULT_WIKI_ORG_ID = "8178808";

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", unique = true)
    private User user;

    @Column(name = "ACCOUNT_EMAIL", length = 255)
    private String accountEmail;

    @Column(name = "OAUTH_TOKEN_ENCRYPTED", length = 4096)
    private String oauthTokenEncrypted;

    @Column(name = "REFRESH_TOKEN_ENCRYPTED", length = 4096)
    private String refreshTokenEncrypted;

    @Column(name = "CALENDAR_BASE_URL", length = 512)
    private String calendarBaseUrl = DEFAULT_CALENDAR_BASE_URL;

    @Column(name = "PERSONAL_CALENDAR_NAME", length = 255)
    private String personalCalendarName;

    @Column(name = "PERSONAL_CALENDAR_PATH", length = 512)
    private String personalCalendarPath;

    @Column(name = "CLIENT_CALENDAR_NAME", length = 255)
    private String clientInterviewCalendarName = DEFAULT_CLIENT_CALENDAR_NAME;

    @Column(name = "CLIENT_CALENDAR_PATH", length = 512)
    private String clientInterviewCalendarPath;

    @Column(name = "DEFAULT_TIME_ZONE", length = 64)
    private String defaultTimeZone = DEFAULT_TIME_ZONE;

    @Column(name = "TELEMOST_BASE_URL", length = 512)
    private String telemostBaseUrl = DEFAULT_TELEMOST_BASE_URL;

    @Column(name = "TELEMOST_AUTO_RECORD")
    private Boolean telemostAutoRecord = true;

    @Column(name = "TELEMOST_AI_SUMMARY")
    private Boolean telemostAiSummary = true;

    @Column(name = "TELEMOST_DEFAULT_ACCESS_LEVEL", length = 64)
    private String telemostDefaultAccessLevel = "anyone";

    @Column(name = "WIKI_BASE_URL", length = 512)
    private String wikiBaseUrl = DEFAULT_WIKI_BASE_URL;

    @Column(name = "WIKI_API_URL", length = 512)
    private String wikiApiUrl = DEFAULT_WIKI_API_URL;

    @Column(name = "WIKI_ORG_ID", length = 64)
    private String wikiOrgId = DEFAULT_WIKI_ORG_ID;

    @Column(name = "WIKI_COLLAB_ID", length = 255)
    private String wikiCollabId;

    @Column(name = "CALENDAR_CONNECTED")
    private Boolean calendarConnected = false;

    @Column(name = "TELEMOST_CONNECTED")
    private Boolean telemostConnected = false;

    @Column(name = "WIKI_CONNECTED")
    private Boolean wikiConnected = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LAST_VERIFIED_AT")
    private Date lastVerifiedAt;

    @Column(name = "LAST_VERIFICATION_MESSAGE", length = 1024)
    private String lastVerificationMessage;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public String getOauthTokenEncrypted() {
        return oauthTokenEncrypted;
    }

    public void setOauthTokenEncrypted(String oauthTokenEncrypted) {
        this.oauthTokenEncrypted = oauthTokenEncrypted;
    }

    public String getRefreshTokenEncrypted() {
        return refreshTokenEncrypted;
    }

    public void setRefreshTokenEncrypted(String refreshTokenEncrypted) {
        this.refreshTokenEncrypted = refreshTokenEncrypted;
    }

    public String getCalendarBaseUrl() {
        return calendarBaseUrl;
    }

    public void setCalendarBaseUrl(String calendarBaseUrl) {
        this.calendarBaseUrl = calendarBaseUrl;
    }

    public String getPersonalCalendarName() {
        return personalCalendarName;
    }

    public void setPersonalCalendarName(String personalCalendarName) {
        this.personalCalendarName = personalCalendarName;
    }

    public String getPersonalCalendarPath() {
        return personalCalendarPath;
    }

    public void setPersonalCalendarPath(String personalCalendarPath) {
        this.personalCalendarPath = personalCalendarPath;
    }

    public String getClientInterviewCalendarName() {
        return clientInterviewCalendarName;
    }

    public void setClientInterviewCalendarName(String clientInterviewCalendarName) {
        this.clientInterviewCalendarName = clientInterviewCalendarName;
    }

    public String getClientInterviewCalendarPath() {
        return clientInterviewCalendarPath;
    }

    public void setClientInterviewCalendarPath(String clientInterviewCalendarPath) {
        this.clientInterviewCalendarPath = clientInterviewCalendarPath;
    }

    public String getDefaultTimeZone() {
        return defaultTimeZone;
    }

    public void setDefaultTimeZone(String defaultTimeZone) {
        this.defaultTimeZone = defaultTimeZone;
    }

    public String getTelemostBaseUrl() {
        return telemostBaseUrl;
    }

    public void setTelemostBaseUrl(String telemostBaseUrl) {
        this.telemostBaseUrl = telemostBaseUrl;
    }

    public Boolean getTelemostAutoRecord() {
        return telemostAutoRecord;
    }

    public void setTelemostAutoRecord(Boolean telemostAutoRecord) {
        this.telemostAutoRecord = telemostAutoRecord;
    }

    public Boolean getTelemostAiSummary() {
        return telemostAiSummary;
    }

    public void setTelemostAiSummary(Boolean telemostAiSummary) {
        this.telemostAiSummary = telemostAiSummary;
    }

    public String getTelemostDefaultAccessLevel() {
        return telemostDefaultAccessLevel;
    }

    public void setTelemostDefaultAccessLevel(String telemostDefaultAccessLevel) {
        this.telemostDefaultAccessLevel = telemostDefaultAccessLevel;
    }

    public String getWikiBaseUrl() {
        return wikiBaseUrl;
    }

    public void setWikiBaseUrl(String wikiBaseUrl) {
        this.wikiBaseUrl = wikiBaseUrl;
    }

    public String getWikiApiUrl() {
        return wikiApiUrl;
    }

    public void setWikiApiUrl(String wikiApiUrl) {
        this.wikiApiUrl = wikiApiUrl;
    }

    public String getWikiOrgId() {
        return wikiOrgId;
    }

    public void setWikiOrgId(String wikiOrgId) {
        this.wikiOrgId = wikiOrgId;
    }

    public String getWikiCollabId() {
        return wikiCollabId;
    }

    public void setWikiCollabId(String wikiCollabId) {
        this.wikiCollabId = wikiCollabId;
    }

    public Boolean getCalendarConnected() {
        return calendarConnected;
    }

    public void setCalendarConnected(Boolean calendarConnected) {
        this.calendarConnected = calendarConnected;
    }

    public Boolean getTelemostConnected() {
        return telemostConnected;
    }

    public void setTelemostConnected(Boolean telemostConnected) {
        this.telemostConnected = telemostConnected;
    }

    public Boolean getWikiConnected() {
        return wikiConnected;
    }

    public void setWikiConnected(Boolean wikiConnected) {
        this.wikiConnected = wikiConnected;
    }

    public Date getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(Date lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public String getLastVerificationMessage() {
        return lastVerificationMessage;
    }

    public void setLastVerificationMessage(String lastVerificationMessage) {
        this.lastVerificationMessage = lastVerificationMessage;
    }
}
