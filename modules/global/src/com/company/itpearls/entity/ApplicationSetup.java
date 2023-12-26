package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;

@Table(name = "ITPEARLS_APPLICATION_SETUP")
@Entity(name = "itpearls_ApplicationSetup")
@NamePattern("%s|name")
public class ApplicationSetup extends StandardEntity {
    private static final long serialVersionUID = -7930326523040472631L;

    @Column(name = "ACTIVE_SETUP")
    private Boolean activeSetup;

    @Column(name = "NAME", length = 128)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "APPLICATION_LOGO_ID")
    private FileDescriptor applicationLogo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "APPLICATION_ICON_ID")
    private FileDescriptor applicationIcon;

    @Column(name = "TELEGRAM_BOT_NAME", length = 128)
    private String telegramBotName;

    @Column(name = "TELEGRAM_BOT_START")
    private Boolean telegramBotStart;

    @Column(name = "TELEGRAM_TOKEN", length = 128)
    private String telegramToken;

    @Column(name = "TELEGRAM_CHAT_OPEN_POSITION", length = 128)
    private String telegramChatOpenPosition;

    @Column(name = "TELEGRAM_CHAT_JOB_CANDIDATE", length = 128)
    private String telegramChatJobCandidate;

    public Boolean getTelegramBotStart() {
        return telegramBotStart;
    }

    public void setTelegramBotStart(Boolean telegramBotStart) {
        this.telegramBotStart = telegramBotStart;
    }

    public String getTelegramBotName() {
        return telegramBotName;
    }

    public void setTelegramBotName(String telegramBotName) {
        this.telegramBotName = telegramBotName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTelegramChatJobCandidate() {
        return telegramChatJobCandidate;
    }

    public void setTelegramChatJobCandidate(String telegramChatJobCandidate) {
        this.telegramChatJobCandidate = telegramChatJobCandidate;
    }

    public Boolean getActiveSetup() {
        return activeSetup;
    }

    public void setActiveSetup(Boolean activeSetup) {
        this.activeSetup = activeSetup;
    }

    public String getTelegramChatOpenPosition() {
        return telegramChatOpenPosition;
    }

    public void setTelegramChatOpenPosition(String telegramChatOpenPosition) {
        this.telegramChatOpenPosition = telegramChatOpenPosition;
    }

    public String getTelegramToken() {
        return telegramToken;
    }

    public void setTelegramToken(String telegramToken) {
        this.telegramToken = telegramToken;
    }

    public FileDescriptor getApplicationIcon() {
        return applicationIcon;
    }

    public void setApplicationIcon(FileDescriptor applicationIcon) {
        this.applicationIcon = applicationIcon;
    }

    public FileDescriptor getApplicationLogo() {
        return applicationLogo;
    }

    public void setApplicationLogo(FileDescriptor applicationLogo) {
        this.applicationLogo = applicationLogo;
    }
}