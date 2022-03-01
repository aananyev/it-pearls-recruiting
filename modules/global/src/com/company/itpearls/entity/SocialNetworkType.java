package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@NamePattern("%s|socialNetwork")
@Table(name = "ITPEARLS_SOCIAL_NETWORK_TYPE", indexes = {
        @Index(name = "IDX_ITPEARLS_SOCIAL_NETWORK_TYPE_SOCIAL_NETWORK", columnList = "SOCIAL_NETWORK")
})
@Entity(name = "itpearls_SocialNetworkType")
public class SocialNetworkType extends StandardEntity {
    private static final long serialVersionUID = -6627736466802346591L;

    @NotNull
    @Column(name = "SOCIAL_NETWORK", nullable = false, length = 30)
    protected String socialNetwork;

    @Column(name = "SOCIAL_NETWORK_URL", length = 80)
    protected String socialNetworkURL;

    @Column(name = "COMMENT_")
    protected String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSocialNetworkURL() {
        return socialNetworkURL;
    }

    public void setSocialNetworkURL(String socialNetworkURL) {
        this.socialNetworkURL = socialNetworkURL;
    }

    public String getSocialNetwork() {
        return socialNetwork;
    }

    public void setSocialNetwork(String socialNetwork) {
        this.socialNetwork = socialNetwork;
    }
}