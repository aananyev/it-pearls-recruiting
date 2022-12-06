package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Table(name = "ITPEARLS_GRADE")
@Entity(name = "itpearls_Grade")
public class Grade extends StandardEntity {
    private static final long serialVersionUID = -2883382796090234777L;

    @Column(name = "GRADE_NAME", unique = true, length = 80)
    private String gradeName;

    public String getGradeName() {
        return gradeName;
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }
}