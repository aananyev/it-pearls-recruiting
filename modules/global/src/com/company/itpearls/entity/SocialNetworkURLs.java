package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@NamePattern("%s|networkName")
@Table(name = "ITPEARLS_SOCIAL_NETWORK_UR_LS")
@Entity(name = "itpearls_SocialNetworkURLs")
public class SocialNetworkURLs extends StandardEntity {
    private static final long serialVersionUID = -4652381238943479311L;

    @NotNull
    @Column(name = "NETWORK_NAME", nullable = false, unique = true, length = 80)
    protected String networkName;

    @NotNull
    @Column(name = "NETWORK_URLS", nullable = false, unique = true, length = 80)
    protected String networkURLS;

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