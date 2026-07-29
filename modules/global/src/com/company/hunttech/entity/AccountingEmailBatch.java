package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Table(name = "HUNTTECH_ACCOUNTING_EMAIL_BATCH", indexes = {
        @Index(name = "IDX_HUNTTECH_ACC_EMAIL_BATCH_FLOW", columnList = "FLOW_TYPE"),
        @Index(name = "IDX_HUNTTECH_ACC_EMAIL_BATCH_STATUS", columnList = "STATUS"),
        @Index(name = "IDX_HUNTTECH_ACC_EMAIL_BATCH_SENDER", columnList = "SENDER_USER_ID")
})
@Entity(name = "hunttech_AccountingEmailBatch")
@NamePattern("%s %s|subject,sentAt")
public class AccountingEmailBatch extends StandardEntity {
    private static final long serialVersionUID = -8594996839142998896L;

    @NotNull
    @Column(name = "FLOW_TYPE", nullable = false)
    private Integer flowType;

    @Temporal(TemporalType.DATE)
    @Column(name = "PERIOD_DATE")
    private Date periodDate;

    @Column(name = "PERIOD_YEAR")
    private Integer periodYear;

    @Column(name = "PERIOD_MONTH")
    private Integer periodMonth;

    @NotNull
    @Column(name = "STATUS", nullable = false)
    private Integer status;

    @NotNull
    @Column(name = "SUBJECT", nullable = false, length = 255)
    private String subject;

    @Lob
    @Column(name = "BODY")
    private String body;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SENDER_USER_ID")
    private ExtUser senderUser;

    @Column(name = "SENDER_EMAIL", length = 255)
    private String senderEmail;

    @Lob
    @Column(name = "TO_EMAILS")
    private String toEmails;

    @Lob
    @Column(name = "CC_EMAILS")
    private String ccEmails;

    @Lob
    @Column(name = "BCC_EMAILS")
    private String bccEmails;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "SENT_AT")
    private Date sentAt;

    @Column(name = "YANDEX_MAIL_MESSAGE_ID", length = 255)
    private String yandexMailMessageId;

    @Lob
    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    public AccountingFlowType getFlowType() {
        return AccountingFlowType.fromId(flowType);
    }

    public void setFlowType(AccountingFlowType flowType) {
        this.flowType = flowType == null ? null : flowType.getId();
    }

    public AccountingEmailBatchStatus getStatus() {
        return AccountingEmailBatchStatus.fromId(status);
    }

    public void setStatus(AccountingEmailBatchStatus status) {
        this.status = status == null ? null : status.getId();
    }

    public Date getPeriodDate() {
        return periodDate;
    }

    public void setPeriodDate(Date periodDate) {
        this.periodDate = periodDate;
    }

    public Integer getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(Integer periodYear) {
        this.periodYear = periodYear;
    }

    public Integer getPeriodMonth() {
        return periodMonth;
    }

    public void setPeriodMonth(Integer periodMonth) {
        this.periodMonth = periodMonth;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public ExtUser getSenderUser() {
        return senderUser;
    }

    public void setSenderUser(ExtUser senderUser) {
        this.senderUser = senderUser;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getToEmails() {
        return toEmails;
    }

    public void setToEmails(String toEmails) {
        this.toEmails = toEmails;
    }

    public String getCcEmails() {
        return ccEmails;
    }

    public void setCcEmails(String ccEmails) {
        this.ccEmails = ccEmails;
    }

    public String getBccEmails() {
        return bccEmails;
    }

    public void setBccEmails(String bccEmails) {
        this.bccEmails = bccEmails;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public String getYandexMailMessageId() {
        return yandexMailMessageId;
    }

    public void setYandexMailMessageId(String yandexMailMessageId) {
        this.yandexMailMessageId = yandexMailMessageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
