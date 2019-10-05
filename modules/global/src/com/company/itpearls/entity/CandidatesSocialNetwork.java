package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@NamePattern("%s %s|candidate,socialNetwork")
@Table(name = "ITPEARLS_CANDIDATES_SOCIAL_NETWORK")
@Entity(name = "itpearls_CandidatesSocialNetwork")
public class CandidatesSocialNetwork extends StandardEntity {
    private static final long serialVersionUID = -5987702190163593102L;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CANDIDATE_ID")
    protected JobCandidate candidate;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SOCIAL_NETWORK_ID")
    protected SocialNetworkURLs socialNetwork;

    @NotNull
    @Column(name = "SOCIAL_NETWORK_UPL", nullable = false, unique = true, length = 80)
    protected String socialNetworkUPL;

    public String getSocialNetworkUPL() {
        return socialNetworkUPL;
    }

    public void setSocialNetworkUPL(String socialNetworkUPL) {
        this.socialNetworkUPL = socialNetworkUPL;
    }

    public SocialNetworkURLs getSocialNetwork() {
        return socialNetwork;
    }

    public void setSocialNetwork(SocialNetworkURLs socialNetwork) {
        this.socialNetwork = socialNetwork;
    }

    public JobCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(JobCandidate candidate) {
        this.candidate = candidate;
    }
}