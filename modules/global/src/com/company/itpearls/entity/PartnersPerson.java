package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity(name = "itpearls_PartnersPerson")
public class PartnersPerson extends Person {
    private static final long serialVersionUID = -7803611965223420315L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @MetaProperty
    @Transient
    private Partners partner;

    public Partners getPartner() {
        return partner;
    }

    public void setPartner(Partners partner) {
        this.partner = partner;
    }
}