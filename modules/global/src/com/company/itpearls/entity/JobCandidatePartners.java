package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "itpearls_JobCandidatePartners")
@NamePattern("%s / %s|fullName,partners")
public class JobCandidatePartners extends JobCandidate {
    private static final long serialVersionUID = 4941483574772925016L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARTNERS_ID")
    private Partners partners;

    public Partners getPartners() {
        return partners;
    }

    public void setPartners(Partners partners) {
        this.partners = partners;
    }
}