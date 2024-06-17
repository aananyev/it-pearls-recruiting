package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import java.util.List;

@Entity(name = "itpearls_Partners")
@NamePattern("%s|comanyName")
public class Partners extends Company {
    private static final long serialVersionUID = 7503381206518453089L;

    @Column(name = "SIGN_PARTNER")
    private Boolean signPartner;

    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "partners")
    private List<PartnersPerson> partnersPerson;

    @JoinTable(name = "ITPEARLS_OPEN_POSITION_PARTNERS_PARTNERS_LINK",
            joinColumns = @JoinColumn(name = "PARTNERS_ID"),
            inverseJoinColumns = @JoinColumn(name = "OPEN_POSITION_PARTNERS_ID"))
    @ManyToMany
    private List<OpenPositionPartners> openPositionPartnerses;

    public List<OpenPositionPartners> getOpenPositionPartnerses() {
        return openPositionPartnerses;
    }

    public void setOpenPositionPartnerses(List<OpenPositionPartners> openPositionPartnerses) {
        this.openPositionPartnerses = openPositionPartnerses;
    }

    public List<PartnersPerson> getPartnersPerson() {
        return partnersPerson;
    }

    public void setPartnersPerson(List<PartnersPerson> partnersPerson) {
        this.partnersPerson = partnersPerson;
    }

    public Boolean getSignPartner() {
        return signPartner;
    }

    public void setSignPartner(Boolean signPartner) {
        this.signPartner = signPartner;
    }
}