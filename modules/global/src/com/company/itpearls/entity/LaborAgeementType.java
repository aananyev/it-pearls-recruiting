package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_LABOR_AGEEMENT_TYPE")
@Entity(name = "itpearls_LaborAgeementType")
@NamePattern("%s|nameAgreement")
public class LaborAgeementType extends StandardEntity {
    private static final long serialVersionUID = -9202105476505288640L;

    @Column(name = "NAME_AGREEMENT", nullable = false, length = 80)
    @NotNull
    private String nameAgreement;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getNameAgreement() {
        return nameAgreement;
    }

    public void setNameAgreement(String nameAgreement) {
        this.nameAgreement = nameAgreement;
    }
}