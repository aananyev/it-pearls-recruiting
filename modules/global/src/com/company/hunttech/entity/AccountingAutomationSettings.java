package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "HUNTTECH_ACCOUNTING_AUTOMATION_SETTINGS", indexes = {
        @Index(name = "IDX_HUNTTECH_ACC_AUTOMATION_ACTIVE", columnList = "ACTIVE")
})
@Entity(name = "hunttech_AccountingAutomationSettings")
@NamePattern("%s|name")
public class AccountingAutomationSettings extends StandardEntity {
    private static final long serialVersionUID = -6443317701635110591L;

    @NotNull
    @Column(name = "NAME", nullable = false, length = 255)
    private String name;

    @Column(name = "ACTIVE")
    private Boolean active = true;

    @Column(name = "TELEGRAM_BOT_USERNAME", length = 128)
    private String telegramBotUsername;

    @Column(name = "CONFIRMATION_TELEGRAM_USER_ID", length = 64)
    private String confirmationTelegramUserId;

    @Column(name = "YANDEX_DISK_ROOT_PATH", length = 1000)
    private String yandexDiskRootPath;

    @Column(name = "INCOMING_SCANS_PATH", length = 500)
    private String incomingScansPath;

    @Column(name = "PRIMARY_DOCUMENTS_PATH", length = 500)
    private String primaryDocumentsPath;

    @Column(name = "ADVANCE_REPORTS_PATH", length = 500)
    private String advanceReportsPath;

    @Column(name = "PRIMARY_SEND_SCHEDULE", length = 128)
    private String primarySendSchedule;

    @Column(name = "ADVANCE_REPORT_SEND_SCHEDULE", length = 128)
    private String advanceReportSendSchedule;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SENDER_USER_ID")
    private ExtUser senderUser;

    @Column(name = "PRIMARY_SUBJECT_PATTERN", length = 255)
    private String primarySubjectPattern;

    @Column(name = "ADVANCE_REPORT_SUBJECT_PATTERN", length = 255)
    private String advanceReportSubjectPattern;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getTelegramBotUsername() {
        return telegramBotUsername;
    }

    public void setTelegramBotUsername(String telegramBotUsername) {
        this.telegramBotUsername = telegramBotUsername;
    }

    public String getConfirmationTelegramUserId() {
        return confirmationTelegramUserId;
    }

    public void setConfirmationTelegramUserId(String confirmationTelegramUserId) {
        this.confirmationTelegramUserId = confirmationTelegramUserId;
    }

    public String getYandexDiskRootPath() {
        return yandexDiskRootPath;
    }

    public void setYandexDiskRootPath(String yandexDiskRootPath) {
        this.yandexDiskRootPath = yandexDiskRootPath;
    }

    public String getIncomingScansPath() {
        return incomingScansPath;
    }

    public void setIncomingScansPath(String incomingScansPath) {
        this.incomingScansPath = incomingScansPath;
    }

    public String getPrimaryDocumentsPath() {
        return primaryDocumentsPath;
    }

    public void setPrimaryDocumentsPath(String primaryDocumentsPath) {
        this.primaryDocumentsPath = primaryDocumentsPath;
    }

    public String getAdvanceReportsPath() {
        return advanceReportsPath;
    }

    public void setAdvanceReportsPath(String advanceReportsPath) {
        this.advanceReportsPath = advanceReportsPath;
    }

    public String getPrimarySendSchedule() {
        return primarySendSchedule;
    }

    public void setPrimarySendSchedule(String primarySendSchedule) {
        this.primarySendSchedule = primarySendSchedule;
    }

    public String getAdvanceReportSendSchedule() {
        return advanceReportSendSchedule;
    }

    public void setAdvanceReportSendSchedule(String advanceReportSendSchedule) {
        this.advanceReportSendSchedule = advanceReportSendSchedule;
    }

    public ExtUser getSenderUser() {
        return senderUser;
    }

    public void setSenderUser(ExtUser senderUser) {
        this.senderUser = senderUser;
    }

    public String getPrimarySubjectPattern() {
        return primarySubjectPattern;
    }

    public void setPrimarySubjectPattern(String primarySubjectPattern) {
        this.primarySubjectPattern = primarySubjectPattern;
    }

    public String getAdvanceReportSubjectPattern() {
        return advanceReportSubjectPattern;
    }

    public void setAdvanceReportSubjectPattern(String advanceReportSubjectPattern) {
        this.advanceReportSubjectPattern = advanceReportSubjectPattern;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
