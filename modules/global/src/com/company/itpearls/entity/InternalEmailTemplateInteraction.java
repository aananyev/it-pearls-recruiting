package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@javax.persistence.DiscriminatorValue("0")
@Entity(name = "itpearls_InternalEmailTemplateInteraction")
public class InternalEmailTemplateInteraction extends InternalEmailTemplate {
    private static final long serialVersionUID = -6492956563726065809L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @OnDeleteInverse(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INTERACTION_ID")
    private Iteraction interaction;

    public Iteraction getInteraction() {
        return interaction;
    }

    public void setInteraction(Iteraction interaction) {
        this.interaction = interaction;
    }
}