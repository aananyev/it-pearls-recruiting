package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;

@Table(name = "ITPEARLS_SOME_FILES")
@Entity(name = "itpearls_SomeFiles")
public class SomeFiles extends StandardEntity {
    private static final long serialVersionUID = 5350354177979238926L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_DESCRIPTOR_ID")
    protected FileDescriptor fileDescriptor;

    @Column(name = "FILE_DESCRIPTION", length = 80)
    protected String fileDescription;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_OWNER_ID")
    protected User fileOwner;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_TYPE_ID")
    protected FileType fileType;

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public User getFileOwner() {
        return fileOwner;
    }

    public void setFileOwner(User fileOwner) {
        this.fileOwner = fileOwner;
    }

    public String getFileDescription() {
        return fileDescription;
    }

    public void setFileDescription(String fileDescription) {
        this.fileDescription = fileDescription;
    }

    public FileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }

    public void setFileDescriptor(FileDescriptor fileDescriptor) {
        this.fileDescriptor = fileDescriptor;
    }
}