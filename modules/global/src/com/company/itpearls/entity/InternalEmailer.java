package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Table(name = "ITPEARLS_INTERNAL_EMAILER")
@Entity(name = "itpearls_InternalEmailer")
@NamePattern("from: %s to: %s (%s)|fromEmail,toEmail,dateSendEmail")
public class InternalEmailer extends StandardEntity {
    private static final long serialVersionUID = 1989669444636246337L;

    @Column(name = "DRAFT_EMAIL")
    private Boolean draftEmail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FROM_EMAIL_ID")
    @NotNull
    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    private ExtUser fromEmail;

    @JoinColumn(name = "TO_EMAIL_ID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @NotNull
    private JobCandidate toEmail;

    @NotNull
    @Column(name = "SUBJECT_EMAIL", nullable = false)
    private String subjectEmail;

    @NotNull
    @Lob
    @Column(name = "BODY_EMAIL", nullable = false)
    private String bodyEmail;

    @Column(name = "BODY_HTML")
    private Boolean bodyHtml;

    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    @Column(name = "DATE_CREATE_EMAIL", nullable = false)
    private Date dateCreateEmail;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE_SEND_EMAIL")
    private Date dateSendEmail;

    public Date getDateSendEmail() {
        return dateSendEmail;
    }

    public void setDateSendEmail(Date dateSendEmail) {
        this.dateSendEmail = dateSendEmail;
    }

    public Boolean getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(Boolean bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public void setToEmail(JobCandidate toEmail) {
        this.toEmail = toEmail;
    }

    public JobCandidate getToEmail() {
        return toEmail;
    }

    public void setFromEmail(ExtUser fromEmail) {
        this.fromEmail = fromEmail;
    }

    public ExtUser getFromEmail() {
        return fromEmail;
    }

    public Date getDateCreateEmail() {
        return dateCreateEmail;
    }

    public void setDateCreateEmail(Date dateCreateEmail) {
        this.dateCreateEmail = dateCreateEmail;
    }

    public Boolean getDraftEmail() {
        return draftEmail;
    }

    public void setDraftEmail(Boolean draftEmail) {
        this.draftEmail = draftEmail;
    }

    public String getBodyEmail() {
        return bodyEmail;
    }

    public void setBodyEmail(String bodyEmail) {
        this.bodyEmail = bodyEmail;
    }

    public String getSubjectEmail() {
        return subjectEmail;
    }

    public void setSubjectEmail(String subjectEmail) {
        this.subjectEmail = subjectEmail;
    }

}