package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.CaseConversion;
import com.haulmont.cuba.core.entity.annotation.ConversionType;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@NamePattern("%s|fileDescription")
@Table(name = "ITPEARLS_SOME_FILES", indexes = {
        @Index(name = "IDX_ITPEARLS_SOME_FILES_DESCRIPTION", columnList = "FILE_DESCRIPTION")
})
@Entity(name = "itpearls_SomeFiles")
public class SomeFiles extends StandardEntity {
    private static final long serialVersionUID = 5350354177979238926L;

    @NotNull
    @Column(name = "FILE_DESCRIPTION", nullable = false, length = 80)
    protected String fileDescription;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_DESCRIPTOR_ID")
    protected FileDescriptor fileDescriptor;

    @CaseConversion(type = ConversionType.LOWER)
    @Column(name = "FILE_LINK")
    protected String fileLink;

    @Lob
    @Column(name = "FILE_COMMENT")
    protected String fileComment;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FILE_OWNER_ID")
    @NotNull
    protected ExtUser fileOwner;

    @NotNull
    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FILE_TYPE_ID")
    protected FileType fileType;

    public void setFileOwner(ExtUser fileOwner) {
        this.fileOwner = fileOwner;
    }

    public ExtUser getFileOwner() {
        return fileOwner;
    }

    public String getFileLink() {
        return fileLink;
    }

    public void setFileLink(String fileLink) {
        this.fileLink = fileLink;
    }

    public void setFileDescription(String fileDescription) {
        this.fileDescription = fileDescription;
    }

    public String getFileComment() {
        return fileComment;
    }

    public void setFileComment(String fileComment) {
        this.fileComment = fileComment;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public String getFileDescription() {
        return fileDescription;
    }

    public FileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }

    public void setFileDescriptor(FileDescriptor fileDescriptor) {
        this.fileDescriptor = fileDescriptor;
    }
}