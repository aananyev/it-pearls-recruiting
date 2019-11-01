package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.CaseConversion;
import com.haulmont.cuba.core.entity.annotation.ConversionType;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@NamePattern("%s|candidate")
@Table(name = "ITPEARLS_CANDIDATE_CV")
@Entity(name = "itpearls_CandidateCV")
public class CandidateCV extends StandardEntity {
    private static final long serialVersionUID = 7346397128043882179L;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CANDIDATE_ID")
    protected JobCandidate candidate;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESUME_POSITION_ID")
    protected Position resumePosition;

    @Lob
    @Column(name = "TEXT_CV")
    protected String textCV;

    @Lob
    @Column(name = "LETTER")
    protected String letter;

    @Transient
    @MetaProperty
    @CaseConversion(type = ConversionType.LOWER)
    protected String lintToCloudFile;

    @Column(name = "LINK_IT_PEARLS_CV")
    protected String linkItPearlsCV;

    @Column(name = "LINK_ORIGINAL_CV")
    protected String linkOriginalCv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_CV_ID")
    protected FileDescriptor fileCV;

    @Temporal(TemporalType.DATE)
    @NotNull
    @Column(name = "DATE_POST", nullable = false)
    protected Date datePost;

    public String getLinkOriginalCv() {
        return linkOriginalCv;
    }

    public void setLinkOriginalCv(String linkOriginalCv) {
        this.linkOriginalCv = linkOriginalCv;
    }

    public String getLinkItPearlsCV() {
        return linkItPearlsCV;
    }

    public void setLinkItPearlsCV(String linkItPearlsCV) {
        this.linkItPearlsCV = linkItPearlsCV;
    }

    public FileDescriptor getFileCV() {
        return fileCV;
    }

    public void setFileCV(FileDescriptor fileCV) {
        this.fileCV = fileCV;
    }

    public String getLetter() {
        return letter;
    }

    public void setLetter(String letter) {
        this.letter = letter;
    }

    public Position getResumePosition() {
        return resumePosition;
    }

    public void setResumePosition(Position resumePosition) {
        this.resumePosition = resumePosition;
    }

    public String getLintToCloudFile() {
        return lintToCloudFile;
    }

    public void setLintToCloudFile(String lintToCloudFile) {
        this.lintToCloudFile = lintToCloudFile;
    }

    public Date getDatePost() {
        return datePost;
    }

    public void setDatePost(Date datePost) {
        this.datePost = datePost;
    }

    public String getTextCV() {
        return textCV;
    }

    public void setTextCV(String textCV) {
        this.textCV = textCV;
    }

    public JobCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(JobCandidate candidate) {
        this.candidate = candidate;
    }
}