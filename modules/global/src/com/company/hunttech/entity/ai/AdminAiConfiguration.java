package com.company.hunttech.entity.ai;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Корпоративное AI-подключение HRM HuntTech.
 *
 * Сущность системного уровня отделяет административные credentials от персональных
 * UserAiConfiguration. Поле apiKeyEncrypted хранит только шифротекст; расшифровка
 * выполняется исключительно в core через AiSecretService непосредственно перед вызовом API.
 */
@SystemLevel
@Table(name = "HUNTTECH_ADMIN_AI_CONFIGURATION", indexes = {
        @Index(name = "IDX_HUNTTECH_ADMIN_AI_CONFIG_PROVIDER", columnList = "PROVIDER_CODE"),
        @Index(name = "IDX_HUNTTECH_ADMIN_AI_CONFIG_ACTIVE", columnList = "IS_ACTIVE")
})
@Entity(name = "hunttech_AdminAiConfiguration")
@NamePattern("%s|name")
public class AdminAiConfiguration extends StandardEntity {
    private static final long serialVersionUID = 3272436640348360781L;

    @NotNull
    @Column(name = "NAME", nullable = false)
    private String name;

    @NotNull
    @Column(name = "PROVIDER_CODE", nullable = false, length = 64)
    private String providerCode;

    @Column(name = "API_KEY_ENCRYPTED", length = 4096)
    private String apiKeyEncrypted;

    @Column(name = "DEFAULT_MODEL_NAME", length = 128)
    private String defaultModelName;

    @Column(name = "BASE_API_URL", length = 512)
    private String baseApiUrl;

    @Column(name = "IS_ACTIVE")
    private Boolean active = true;

    @Column(name = "PRIORITY_")
    private Integer priority = 0;

    @Column(name = "LAST_TEST_STATUS", length = 32)
    private String lastTestStatus;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LAST_TEST_AT")
    private Date lastTestAt;

    @Column(name = "LAST_ERROR", length = 1000)
    private String lastError;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
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

    public String getBaseApiUrl() {
        return baseApiUrl;
    }

    public void setBaseApiUrl(String baseApiUrl) {
        this.baseApiUrl = baseApiUrl;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getLastTestStatus() {
        return lastTestStatus;
    }

    public void setLastTestStatus(String lastTestStatus) {
        this.lastTestStatus = lastTestStatus;
    }

    public Date getLastTestAt() {
        return lastTestAt;
    }

    public void setLastTestAt(Date lastTestAt) {
        this.lastTestAt = lastTestAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
