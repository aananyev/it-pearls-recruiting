package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;

@DiscriminatorValue("0")
@Entity(name = "itpearls_InternalEmailerTemplate")
@NamePattern("%s %s %s|fromEmail,toEmail,dateSendEmail")
public class InternalEmailerTemplate extends InternalEmailer {
    private static final long serialVersionUID = 1487827434848726699L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMAIL_TEMPLATE_ID")
    private InternalEmailTemplate emailTemplate;

    public InternalEmailTemplate getEmailTemplate() {
        return emailTemplate;
    }

    public void setEmailTemplate(InternalEmailTemplate emailTemplate) {
        this.emailTemplate = emailTemplate;
    }
}