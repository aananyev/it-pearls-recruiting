package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_OWNERSHUP")
@Entity(name = "itpearls_Ownershup")
public class Ownershup extends StandardEntity {
    private static final long serialVersionUID = -6057344078066436306L;

    @NotNull
    @Column(name = "SHORT_TYPE", nullable = false, unique = true, length = 7)
    protected String shortType;

    @NotNull
    @Column(name = "LONG_TYPE", nullable = false, unique = true, length = 30)
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