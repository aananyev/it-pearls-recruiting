package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;

@Table(name = "ITPEARLS_APPLICATION_SETUP")
@Entity(name = "itpearls_ApplicationSetup")
public class ApplicationSetup extends StandardEntity {
    private static final long serialVersionUID = -7930326523040472631L;

    @Column(name = "ACTIVE_SETUP", unique = true)
    private Boolean activeSetup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "APPLICATION_LOGO_ID")
    private FileDescriptor applicationLogo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "APPLICATION_ICON_ID")
    private FileDescriptor applicationIcon;

    @Column(name = "TELEGRAM_TOKEN", length = 128)
    private String telegramToken;

    @Column(name = "TELEGRAM_CHAT_OPEN_POSITION", length = 128)
    private String telegramChatOpenPosition;

    @Column(name = "TELEGRAM_CHAT_JOB_CANDIDATE", length = 128)
    private String telegramChatJobCandidate;

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