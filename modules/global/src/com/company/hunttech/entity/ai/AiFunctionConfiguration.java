package com.company.hunttech.entity.ai;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * Централизованная конфигурация одной бизнес-функции AI.
 *
 * Сущность хранит стабильный function code, prompt, capability и правила маршрутизации.
 * Бизнес-экраны должны знать только code и передавать контекст в AiExecutionService.
 */
@Table(name = "HUNTTECH_AI_FUNCTION_CONFIGURATION", indexes = {
        @Index(name = "IDX_HUNTTECH_AI_FUNCTION_CODE", columnList = "CODE", unique = true),
        @Index(name = "IDX_HUNTTECH_AI_FUNCTION_ACTIVE", columnList = "IS_ACTIVE")
})
@Entity(name = "hunttech_AiFunctionConfiguration")
@NamePattern("%s|name")
public class AiFunctionConfiguration extends StandardEntity {
    private static final long serialVersionUID = 4685096859147521381L;

    @NotNull
    @Column(name = "CODE", nullable = false, length = 64, unique = true)
    private String code;

    @NotNull
    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @NotNull
    @Column(name = "CAPABILITY", nullable = false, length = 32)
    private String capability;

    @Lob
    @Column(name = "SYSTEM_PROMPT")
    private String systemPrompt;

    @Lob
    @Column(name = "PROMPT_TEMPLATE")
    private String promptTemplate;

    @Column(name = "TEMPERATURE")
    private Double temperature = 0.7;

    @Column(name = "MAX_TOKENS")
    private Integer maxTokens;

    /** Common calendar-month chat quota; user-specific override has priority. */
    @Column(name = "DEFAULT_MONTHLY_TOKEN_QUOTA")
    private Integer defaultMonthlyTokenQuota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADMIN_CONFIGURATION_ID")
    private AdminAiConfiguration adminConfiguration;

    @Column(name = "ADMIN_MODEL_NAME", length = 128)
    private String adminModelName;

    @NotNull
    @Column(name = "EXECUTION_POLICY", nullable = false, length = 32)
    private String executionPolicy;

    @NotNull
    @Column(name = "FALLBACK_POLICY", nullable = false, length = 32)
    private String fallbackPolicy;

    @Column(name = "ALLOW_MODEL_OVERRIDE")
    private Boolean allowModelOverride = false;

    @Column(name = "IS_ACTIVE")
    private Boolean active = true;

    /**
     * Флаг передачи пользовательского контекста («Обо мне», UserAiProfile) в system prompt
     * этой функции при текстовых вызовах. NULL трактуется execution layer'ом по capability:
     * текстовые — true, IMAGE — false. IMAGE-путь в v1 контекст не получает независимо от флага.
     */
    @Column(name = "INCLUDE_USER_CONTEXT")
    private Boolean includeUserContext;

    @Column(name = "CONFIGURATION_VERSION")
    private Integer configurationVersion = 1;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AiCapability getCapability() {
        return AiCapability.fromId(capability);
    }

    public void setCapability(AiCapability capability) {
        this.capability = capability == null ? null : capability.getId();
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Integer getDefaultMonthlyTokenQuota() {
        return defaultMonthlyTokenQuota;
    }

    public void setDefaultMonthlyTokenQuota(Integer defaultMonthlyTokenQuota) {
        this.defaultMonthlyTokenQuota = defaultMonthlyTokenQuota;
    }

    public AdminAiConfiguration getAdminConfiguration() {
        return adminConfiguration;
    }

    public void setAdminConfiguration(AdminAiConfiguration adminConfiguration) {
        this.adminConfiguration = adminConfiguration;
    }

    public String getAdminModelName() {
        return adminModelName;
    }

    public void setAdminModelName(String adminModelName) {
        this.adminModelName = adminModelName;
    }

    public AiExecutionPolicy getExecutionPolicy() {
        return AiExecutionPolicy.fromId(executionPolicy);
    }

    public void setExecutionPolicy(AiExecutionPolicy executionPolicy) {
        this.executionPolicy = executionPolicy == null ? null : executionPolicy.getId();
    }

    public AiFallbackPolicy getFallbackPolicy() {
        return AiFallbackPolicy.fromId(fallbackPolicy);
    }

    public void setFallbackPolicy(AiFallbackPolicy fallbackPolicy) {
        this.fallbackPolicy = fallbackPolicy == null ? null : fallbackPolicy.getId();
    }

    public Boolean getAllowModelOverride() {
        return allowModelOverride;
    }

    public void setAllowModelOverride(Boolean allowModelOverride) {
        this.allowModelOverride = allowModelOverride;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getIncludeUserContext() {
        return includeUserContext;
    }

    public void setIncludeUserContext(Boolean includeUserContext) {
        this.includeUserContext = includeUserContext;
    }

    public Integer getConfigurationVersion() {
        return configurationVersion;
    }

    public void setConfigurationVersion(Integer configurationVersion) {
        this.configurationVersion = configurationVersion;
    }
}
