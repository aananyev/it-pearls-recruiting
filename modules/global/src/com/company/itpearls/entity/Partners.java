package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "itpearls_Partners")
@NamePattern("%s|comanyName")
public class Partners extends Company {
    private static final long serialVersionUID = 7503381206518453089L;

    @Column(name = "SIGN_PARTNER")
    private Boolean signPartner;

    public Boolean getSignPartner() {
        return signPartner;
    }

    public void setSignPartner(Boolean signPartner) {
        this.signPartner = signPartner;
    }
}