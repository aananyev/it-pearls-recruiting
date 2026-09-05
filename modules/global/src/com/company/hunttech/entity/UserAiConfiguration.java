package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "HUNTTECH_USER_AI_CONFIGURATION", indexes = {
        @Index(name = "IDX_HUNTTECH_USER_AI_CONFIGURATION_USER", columnList = "USER_ID")
})
@Entity(name = "hunttech_UserAiConfiguration")
@NamePattern("%s %s|user,providerCode")
public class UserAiConfiguration extends StandardEntity {
    private static final long serialVersionUID = 4829173645019283746L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID")
    private User user;

    @Column(name = "PROVIDER_CODE", length = 64)
    private String providerCode;

    @Column(name = "API_KEY", length = 512)
    /**
     * Transitional legacy field. New writes must use apiKeyEncrypted.
     */
    private String apiKey;

    @Column(name = "API_KEY_ENCRYPTED", length = 4096)
    private String apiKeyEncrypted;

    @Column(name = "DEFAULT_MODEL_NAME", length = 128)
    private String defaultModelName;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive = true;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKeyEncrypted() {
        return apiKeyEncrypted;
    }

    public void setApiKeyEncrypted(String apiKeyEncrypted) {
        this.apiKeyEncrypted = apiKeyEncrypted;
    }

    public String getDefaultModelName() {
        return defaultModelName;
    }

    public void setDefaultModelName(String defaultModelName) {
        this.defaultModelName = defaultModelName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
