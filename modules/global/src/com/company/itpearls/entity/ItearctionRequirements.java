package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_ITEARCTION_REQUIREMENTS")
@Entity(name = "itpearls_ItearctionRequirements")
public class ItearctionRequirements extends StandardEntity {
    private static final long serialVersionUID = 4441950460805719913L;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITERACTION_ID")
    protected Iteraction iteraction;

    @Column(name = "REQUIREMENT")
    @NotNull
    protected Boolean requirement;

    @Column(name = "REQUIREMENT_ALL")
    @NotNull
    protected Boolean requirementAll;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITERACTION_REQUIREMEN_ID")
    protected Iteraction iteractionRequirement;

    public void setRequirementAll(Boolean requirementAll) {
        this.requirementAll = requirementAll;
    }

    public Boolean getRequirementAll() {
        return requirementAll;
    }

    public void setIteractionRequirement(Iteraction iteractionRequirement) {
        this.iteractionRequirement = iteractionRequirement;
    }

    public Iteraction getIteractionRequirement() {
        return iteractionRequirement;
    }

    public void setRequirement(Boolean requirement) {
        this.requirement = requirement;
    }

    public Boolean getRequirement() {
        return requirement;
    }

    public void setIteraction(Iteraction iteraction) {
        this.iteraction = iteraction;
    }

    public Iteraction getIteraction() {
        return iteraction;
    }
}