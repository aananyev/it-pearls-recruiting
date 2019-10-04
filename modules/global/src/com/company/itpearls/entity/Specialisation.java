package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

@NamePattern("%s|specRuName")
@Table(name = "ITPEARLS_SPECIALISATION")
@Entity(name = "itpearls_Specialisation")
public class Specialisation extends StandardEntity {
    private static final long serialVersionUID = 449400131868283935L;

    @Column(name = "SPEC_RU_NAME", unique = true, length = 80)
    protected String specRuName;

    public String getSpecRuName() {
        return specRuName;
    }

    public void setSpecRuName(String specRuName) {
        this.specRuName = specRuName;
    }
}