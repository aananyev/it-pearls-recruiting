package com.company.itpearls.entity;

import javax.persistence.*;

@Entity(name = "itpearls_PartnersPerson")
public class PartnersPerson extends Person {
    private static final long serialVersionUID = -7803611965223420315L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARTNERS_ID")
    private Partners partners;

    @Column(name = "PARTNER_PERSON_LOGIN", unique = true, length = 64)
    private String partnerPersonLogin;

    public Partners getPartners() {
        return partners;
    }

    public void setPartners(Partners partners) {
        this.partners = partners;
    }

    public String getPartnerPersonLogin() {
        return partnerPersonLogin;
    }

    public void setPartnerPersonLogin(String partnerPersonLogin) {
        this.partnerPersonLogin = partnerPersonLogin;
    }

}