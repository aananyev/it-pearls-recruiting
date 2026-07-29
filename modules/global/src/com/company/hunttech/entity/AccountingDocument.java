package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

@Table(name = "HUNTTECH_ACCOUNTING_DOCUMENT", indexes = {
        @Index(name = "IDX_HUNTTECH_ACC_DOCUMENT_COMPANY", columnList = "COMPANY_ID"),
        @Index(name = "IDX_HUNTTECH_ACC_DOCUMENT_BATCH", columnList = "EMAIL_BATCH_ID"),
        @Index(name = "IDX_HUNTTECH_ACC_DOCUMENT_STATUS", columnList = "STATUS"),
        @Index(name = "IDX_HUNTTECH_ACC_DOCUMENT_FLOW", columnList = "FLOW_TYPE"),
        @Index(name = "IDX_HUNTTECH_ACC_DOCUMENT_HASH", columnList = "FILE_HASH")
})
@Entity(name = "hunttech_AccountingDocument")
@NamePattern("%s %s|documentNumber,finalFileName")
public class AccountingDocument extends StandardEntity {
    private static final long serialVersionUID = 5962212388401865159L;

    @NotNull
    @Column(name = "FLOW_TYPE", nullable = false)
    private Integer flowType;

    @NotNull
    @Column(name = "DOCUMENT_TYPE", nullable = false)
    private Integer documentType;

    @NotNull
    @Column(name = "STATUS", nullable = false)
    private Integer status;

    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    @Column(name = "RECEIVED_AT", nullable = false)
    private Date receivedAt;

    @Temporal(TemporalType.DATE)
    @Column(name = "DOCUMENT_DATE")
    private Date documentDate;

    @Column(name = "DOCUMENT_NUMBER", length = 128)
    private String documentNumber;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_ID")
    private Company company;

    @Column(name = "RECOGNIZED_COMPANY_NAME", length = 255)
    private String recognizedCompanyName;

    @Column(name = "RECOGNIZED_INN", length = 16)
    private String recognizedInn;

    @Column(name = "AMOUNT", precision = 19, scale = 2)
    private BigDecimal amount;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CURRENCY_ID")
    private Currency currency;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EXPENSE_CATEGORY_ID")
    private AccountingExpenseCategory expenseCategory;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LABOR_AGREEMENT_ID")
    private LaborAgreement laborAgreement;

    @Column(name = "ORIGINAL_FILE_NAME", length = 255)
    private String originalFileName;

    @Column(name = "FINAL_FILE_NAME", length = 255)
    private String finalFileName;

    @Column(name = "YANDEX_DISK_ORIGINAL_PATH", length = 1000)
    private String yandexDiskOriginalPath;

    @Column(name = "YANDEX_DISK_FINAL_PATH", length = 1000)
    private String yandexDiskFinalPath;

    @Column(name = "FILE_HASH", length = 128)
    private String fileHash;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

    @Column(name = "MIME_TYPE", length = 128)
    private String mimeType;

    @Column(name = "TELEGRAM_CHAT_ID", length = 64)
    private String telegramChatId;

    @Column(name = "TELEGRAM_MESSAGE_ID")
    private Long telegramMessageId;

    @Column(name = "TELEGRAM_FILE_ID", length = 255)
    private String telegramFileId;

    @Column(name = "CONFIRMED_BY_TELEGRAM_USER_ID", length = 64)
    private String confirmedByTelegramUserId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CONFIRMED_AT")
    private Date confirmedAt;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMAIL_BATCH_ID")
    private AccountingEmailBatch emailBatch;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "SENT_AT")
    private Date sentAt;

    @Lob
    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    public AccountingFlowType getFlowType() {
        return AccountingFlowType.fromId(flowType);
    }

    public void setFlowType(AccountingFlowType flowType) {
        this.flowType = flowType == null ? null : flowType.getId();
    }

    public AccountingDocumentType getDocumentType() {
        return AccountingDocumentType.fromId(documentType);
    }

    public void setDocumentType(AccountingDocumentType documentType) {
        this.documentType = documentType == null ? null : documentType.getId();
    }

    public AccountingDocumentStatus getStatus() {
        return AccountingDocumentStatus.fromId(status);
    }

    public void setStatus(AccountingDocumentStatus status) {
        this.status = status == null ? null : status.getId();
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Date getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(Date documentDate) {
        this.documentDate = documentDate;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getRecognizedCompanyName() {
        return recognizedCompanyName;
    }

    public void setRecognizedCompanyName(String recognizedCompanyName) {
        this.recognizedCompanyName = recognizedCompanyName;
    }

    public String getRecognizedInn() {
        return recognizedInn;
    }

    public void setRecognizedInn(String recognizedInn) {
        this.recognizedInn = recognizedInn;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public AccountingExpenseCategory getExpenseCategory() {
        return expenseCategory;
    }

    public void setExpenseCategory(AccountingExpenseCategory expenseCategory) {
        this.expenseCategory = expenseCategory;
    }

    public LaborAgreement getLaborAgreement() {
        return laborAgreement;
    }

    public void setLaborAgreement(LaborAgreement laborAgreement) {
        this.laborAgreement = laborAgreement;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFinalFileName() {
        return finalFileName;
    }

    public void setFinalFileName(String finalFileName) {
        this.finalFileName = finalFileName;
    }

    public String getYandexDiskOriginalPath() {
        return yandexDiskOriginalPath;
    }

    public void setYandexDiskOriginalPath(String yandexDiskOriginalPath) {
        this.yandexDiskOriginalPath = yandexDiskOriginalPath;
    }

    public String getYandexDiskFinalPath() {
        return yandexDiskFinalPath;
    }

    public void setYandexDiskFinalPath(String yandexDiskFinalPath) {
        this.yandexDiskFinalPath = yandexDiskFinalPath;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public Long getTelegramMessageId() {
        return telegramMessageId;
    }

    public void setTelegramMessageId(Long telegramMessageId) {
        this.telegramMessageId = telegramMessageId;
    }

    public String getTelegramFileId() {
        return telegramFileId;
    }

    public void setTelegramFileId(String telegramFileId) {
        this.telegramFileId = telegramFileId;
    }

    public String getConfirmedByTelegramUserId() {
        return confirmedByTelegramUserId;
    }

    public void setConfirmedByTelegramUserId(String confirmedByTelegramUserId) {
        this.confirmedByTelegramUserId = confirmedByTelegramUserId;
    }

    public Date getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Date confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public AccountingEmailBatch getEmailBatch() {
        return emailBatch;
    }

    public void setEmailBatch(AccountingEmailBatch emailBatch) {
        this.emailBatch = emailBatch;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
