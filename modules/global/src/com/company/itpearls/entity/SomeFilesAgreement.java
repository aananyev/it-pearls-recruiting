package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "itpearls_SomeFilesAgreement")
public class SomeFilesAgreement extends SomeFiles {
    private static final long serialVersionUID = 3264636031316601463L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @OnDeleteInverse(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LABOR_AGREEMENT_ID")
    private LaborAgreement laborAgreement;

    public LaborAgreement getLaborAgreement() {
        return laborAgreement;
    }

    public void setLaborAgreement(LaborAgreement laborAgreement) {
        this.laborAgreement = laborAgreement;
    }
}