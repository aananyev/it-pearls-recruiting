package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Table(name = "HUNTTECH_ACCOUNTING_DOCUMENT_EVENT", indexes = {
        @Index(name = "IDX_HUNTTECH_ACC_DOC_EVENT_DOCUMENT", columnList = "DOCUMENT_ID"),
        @Index(name = "IDX_HUNTTECH_ACC_DOC_EVENT_TYPE", columnList = "EVENT_TYPE")
})
@Entity(name = "hunttech_AccountingDocumentEvent")
@NamePattern("%s %s|eventAt,eventType")
public class AccountingDocumentEvent extends StandardEntity {
    private static final long serialVersionUID = 3646439836180343128L;

    @NotNull
    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DOCUMENT_ID")
    private AccountingDocument document;

    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    @Column(name = "EVENT_AT", nullable = false)
    private Date eventAt;

    @NotNull
    @Column(name = "EVENT_TYPE", nullable = false)
    private Integer eventType;

    @Column(name = "OLD_STATUS")
    private Integer oldStatus;

    @Column(name = "NEW_STATUS")
    private Integer newStatus;

    @Lob
    @Column(name = "MESSAGE")
    private String message;

    @Column(name = "SOURCE", length = 64)
    private String source;

    @Column(name = "TELEGRAM_USER_ID", length = 64)
    private String telegramUserId;

    public AccountingDocument getDocument() {
        return document;
    }

    public void setDocument(AccountingDocument document) {
        this.document = document;
    }

    public Date getEventAt() {
        return eventAt;
    }

    public void setEventAt(Date eventAt) {
        this.eventAt = eventAt;
    }

    public AccountingDocumentEventType getEventType() {
        return AccountingDocumentEventType.fromId(eventType);
    }

    public void setEventType(AccountingDocumentEventType eventType) {
        this.eventType = eventType == null ? null : eventType.getId();
    }

    public AccountingDocumentStatus getOldStatus() {
        return AccountingDocumentStatus.fromId(oldStatus);
    }

    public void setOldStatus(AccountingDocumentStatus oldStatus) {
        this.oldStatus = oldStatus == null ? null : oldStatus.getId();
    }

    public AccountingDocumentStatus getNewStatus() {
        return AccountingDocumentStatus.fromId(newStatus);
    }

    public void setNewStatus(AccountingDocumentStatus newStatus) {
        this.newStatus = newStatus == null ? null : newStatus.getId();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTelegramUserId() {
        return telegramUserId;
    }

    public void setTelegramUserId(String telegramUserId) {
        this.telegramUserId = telegramUserId;
    }
}
