package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@NamePattern("%s|shortType")
@Table(name = "ITPEARLS_OWNERSHUP", indexes = {
        @Index(name = "IDX_ITPEARLS_OWNERSHUP_SHORT_TYPE", columnList = "SHORT_TYPE"),
        @Index(name = "IDX_ITPEARLS_OWNERSHUP_LONG_TYPE", columnList = "LONG_TYPE")
})
@Entity(name = "itpearls_Ownershup")
public class Ownershup extends StandardEntity {
    private static final long serialVersionUID = -6057344078066436306L;

    @NotNull
    @Column(name = "SHORT_TYPE", nullable = false, unique = true, length = 7)
    protected String shortType;

    @NotNull
    @Column(name = "LONG_TYPE", nullable = false, unique = true, length = 50)
    protected String longType;

    public String getShortType() {
        return shortType;
    }

    public void setShortType(String shortType) {
        this.shortType = shortType;
    }

    public String getLongType() {
        return longType;
    }

    public void setLongType(String longType) {
        this.longType = longType;
    }
}