package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_RECRUITING_RECRUTIERS")
@Entity(name = "itpearls_RecruitingRecrutiers")
public class RecruitingRecrutiers extends StandardEntity {
    private static final long serialVersionUID = -7055415068756965371L;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RECRUTIER_NAME_ID")
    protected User recrutierName;

    @Lob
    @Column(name = "PASSAGE")
    protected String passage;

    @Column(name = "SEND_PASSAGE")
    protected Boolean sendPassage;

    @Column(name = "CHECK_PASSAGE")
    protected Boolean checkPassage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RECRUTIER_CV_ID")
    protected FileDescriptor recrutierCV;

    public FileDescriptor getRecrutierCV() {
        return recrutierCV;
    }

    public void setRecrutierCV(FileDescriptor recrutierCV) {
        this.recrutierCV = recrutierCV;
    }

    public Boolean getCheckPassage() {
        return checkPassage;
    }

    public void setCheckPassage(Boolean checkPassage) {
        this.checkPassage = checkPassage;
    }

    public Boolean getSendPassage() {
        return sendPassage;
    }

    public void setSendPassage(Boolean sendPassage) {
        this.sendPassage = sendPassage;
    }

    public String getPassage() {
        return passage;
    }

    public void setPassage(String passage) {
        this.passage = passage;
    }

    public User getRecrutierName() {
        return recrutierName;
    }

    public void setRecrutierName(User recrutierName) {
        this.recrutierName = recrutierName;
    }
}