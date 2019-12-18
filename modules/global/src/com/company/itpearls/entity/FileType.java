package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Table(name = "ITPEARLS_FILE_TYPE")
@Entity(name = "itpearls_FileType")
public class FileType extends StandardEntity {
    private static final long serialVersionUID = 2484124663629059989L;

    @Column(name = "NAME_FILE_TYPE", length = 30)
    protected String nameFileType;

    @Column(name = "DECRIPTION_FILE_TYPE", length = 80)
    protected String decriptionFileType;

    public String getDecriptionFileType() {
        return decriptionFileType;
    }

    public void setDecriptionFileType(String decriptionFileType) {
        this.decriptionFileType = decriptionFileType;
    }

    public String getNameFileType() {
        return nameFileType;
    }

    public void setNameFileType(String nameFileType) {
        this.nameFileType = nameFileType;
    }
}