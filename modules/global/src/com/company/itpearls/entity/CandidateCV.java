package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.chile.core.datatypes.impl.EnumClass;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.*;
import com.haulmont.cuba.core.global.DeletePolicy;
import com.haulmont.cuba.security.entity.User;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@NamePattern("%s|candidate")
@Table(name = "ITPEARLS_CANDIDATE_CV", indexes = {
        @Index(name = "IDX_ITPEARLS_CANDIDATE_C_V_DATE_POST", columnList = "DATE_POST")
})
@Entity(name = "itpearls_CandidateCV")
public class CandidateCV extends StandardEntity {
    private static final long serialVersionUID = 7346397128043882179L;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CANDIDATE_ID")
    protected JobCandidate candidate;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESUME_POSITION_ID")
    protected Position resumePosition;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @OnDelete(DeletePolicy.DENY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TO_VACANCY_ID")
    protected OpenPosition toVacancy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OWNER_ID")
    protected User owner;

    @Lob
    @Column(name = "TEXT_CV")
    protected String textCV;

    @Lob
    @Column(name = "LETTER")
    protected String letter;

    @Lob
    @Column(name = "COMMENT_LETTER")
    private String commentLetter;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORIGINAL_FILE_CV_ID")
    protected FileDescriptor originalFileCV;

    @Temporal(TemporalType.DATE)
    @NotNull
    @Column(name = "DATE_POST", nullable = false)
    protected Date datePost;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "candidateCV")
    protected List<SomeFiles> someFiles;


    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "candidateCV")
    protected List<SkillTree> skillTree;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_IMAGE_FACE")
    protected FileDescriptor fileImageFace;

    @Column(name = "IMAGE_BYTE_ARRAY")
    private byte[] imageByteArray;

    public byte[] getImageByteArray() {
        return imageByteArray;
    }

    public void setImageByteArray(byte[] imageByteArray) {
        this.imageByteArray = imageByteArray;
    }

    public String getCommentLetter() {
        return commentLetter;
    }

    public void setCommentLetter(String commentLetter) {
        this.commentLetter = commentLetter;
    }

    public List<SomeFiles> getSomeFiles() {
        return someFiles;
    }

    public void setSomeFiles(List<SomeFiles> someFiles) {
        this.someFiles = someFiles;
    }

    public OpenPosition getToVacancy() {
        return toVacancy;
    }

    public void setToVacancy(OpenPosition toVacancy) {
        this.toVacancy = toVacancy;
    }

    public FileDescriptor getOriginalFileCV() {
        return originalFileCV;
    }

    public void setOriginalFileCV(FileDescriptor originalFileCV) {
        this.originalFileCV = originalFileCV;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

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

    public void setSkillTree(List<SkillTree> skillTree) {
        this.skillTree = skillTree;
    }

    public List<SkillTree> getSkillTree() {
        return skillTree;
    }

    public FileDescriptor getFileImageFace() {
        return fileImageFace;
    }

    public void setFileImageFace(FileDescriptor fileImageFace) {
        this.fileImageFace = fileImageFace;
    }

    public enum SelectedCloseActionType implements EnumClass<String> {

        SELECTED("Selected"),
        DESELECTED("Deselected"),
        UNSELECTED("Unselected"),
        NOTSELECTED("NotSelected");

        private String id;

        SelectedCloseActionType(String value) {
            this.id = value;
        }

        public String getId() {
            return id;
        }

        @Nullable
        public static SelectedCloseActionType fromId(String id) {
            for (SelectedCloseActionType at : SelectedCloseActionType.values()) {
                if (at.getId().equals(id)) {
                    return at;
                }
            }
            return null;
        }
    }
}