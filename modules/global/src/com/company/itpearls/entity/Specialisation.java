package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@NamePattern("%s|specEnName")
@Table(name = "ITPEARLS_SPECIALISATION")
@Entity(name = "itpearls_Specialisation")
public class Specialisation extends StandardEntity {
    private static final long serialVersionUID = 449400131868283935L;

    @Column(name = "SPEC_RU_NAME", unique = true, length = 80)
    protected String specRuName;

    @Column(name = "SPEC_EN_NAME", unique = true, length = 80)
    protected String specEnName;

    public String getSpecEnName() {
        return specEnName;
    }

    public void setSpecEnName(String specEnName) {
        this.specEnName = specEnName;
    }

    public String getSpecRuName() {
        return specRuName;
    }

    public void setSpecRuName(String specRuName) {
        this.specRuName = specRuName;
    }
}