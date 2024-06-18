package com.company.itpearls.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "itpearls_IteractionListPartners")
public class IteractionListPartners extends IteractionList {
    private static final long serialVersionUID = -2881470555749862530L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARTNER_ID")
    private Partners partner;

    public Partners getPartner() {
        return partner;
    }

    public void setPartner(Partners partner) {
        this.partner = partner;
    }
}