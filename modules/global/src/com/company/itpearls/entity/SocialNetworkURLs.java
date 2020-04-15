package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;

@NamePattern("%s|networkName")
@Table(name = "ITPEARLS_SOCIAL_NETWORK_UR_LS")
@Entity(name = "itpearls_SocialNetworkURLs")
public class SocialNetworkURLs extends StandardEntity {
    private static final long serialVersionUID = -4652381238943479311L;

    @Column(name = "NETWORK_NAME", length = 80)
    protected String networkName;

    @Column(name = "NETWORK_URLS", length = 80)
    protected String networkURLS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JOB_CANDIDATE_ID")
    protected JobCandidate jobCandidate;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SOCIAL_NETWORK_URL_ID")
    protected SocialNetworkType socialNetworkURL;

    public SocialNetworkType getSocialNetworkURL() {
        return socialNetworkURL;
    }

    public void setSocialNetworkURL(SocialNetworkType socialNetworkURL) {
        this.socialNetworkURL = socialNetworkURL;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public String getNetworkURLS() {
        return networkURLS;
    }

    public void setNetworkURLS(String networkURLS) {
        this.networkURLS = networkURLS;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }
}