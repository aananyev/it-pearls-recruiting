package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_INTERNAL_EMAIL_TEMPLATE")
@Entity(name = "itpearls_InternalEmailTemplate")
@NamePattern("%s / %s|templateName,templateAuthor")
public class InternalEmailTemplate extends StandardEntity {
    private static final long serialVersionUID = -1764987245585713063L;

    @Column(name = "TEMPLATE_NAME", nullable = false, length = 128)
    @NotNull
    private String templateName;

    @Column(name = "TEMPLATE_SUBJ")
    private String templateSubj;

    @Lob
    @Column(name = "TEMPLATE_TEXT", nullable = false)
    @NotNull
    private String templateText;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEMPLATE_OPEN_POSITION_ID")
    private OpenPosition templateOpenPosition;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEMPLATE_POSITION_ID")
    private Position templatePosition;

    @Column(name = "TEMPLATE_COMMENT")
    private String templateComment;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TEMPLATE_AUTHOR_ID")
    private User templateAuthor;

    @Column(name = "SHARE_TEMPLATE")
    private Boolean shareTemplate;

    public Boolean getShareTemplate() {
        return shareTemplate;
    }

    public void setShareTemplate(Boolean shareTemplate) {
        this.shareTemplate = shareTemplate;
    }

    public String getTemplateSubj() {
        return templateSubj;
    }

    public void setTemplateSubj(String templateSubj) {
        this.templateSubj = templateSubj;
    }

    public Position getTemplatePosition() {
        return templatePosition;
    }

    public void setTemplatePosition(Position templatePosition) {
        this.templatePosition = templatePosition;
    }

    public OpenPosition getTemplateOpenPosition() {
        return templateOpenPosition;
    }

    public void setTemplateOpenPosition(OpenPosition templateOpenPosition) {
        this.templateOpenPosition = templateOpenPosition;
    }

    public User getTemplateAuthor() {
        return templateAuthor;
    }

    public void setTemplateAuthor(User templateAuthor) {
        this.templateAuthor = templateAuthor;
    }

    public String getTemplateComment() {
        return templateComment;
    }

    public void setTemplateComment(String templateComment) {
        this.templateComment = templateComment;
    }

    public String getTemplateText() {
        return templateText;
    }

    public void setTemplateText(String templateText) {
        this.templateText = templateText;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
}