package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Table(name = "ITPEARLS_LABOR_AGEEMENT_TYPE")
@Entity(name = "itpearls_LaborAgeementType")
@NamePattern("%s|nameAgreement")
public class LaborAgeementType extends StandardEntity {
    private static final long serialVersionUID = -9202105476505288640L;

    @Column(name = "NAME_AGREEMENT", length = 80)
    private String nameAgreement;

    public String getNameAgreement() {
        return nameAgreement;
    }

    public void setNameAgreement(String nameAgreement) {
        this.nameAgreement = nameAgreement;
    }
}