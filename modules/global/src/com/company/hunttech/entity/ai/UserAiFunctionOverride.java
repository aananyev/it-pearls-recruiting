package com.company.hunttech.entity.ai;

import com.company.hunttech.entity.UserAiConfiguration;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

/**
 * Персональное замещение корпоративного подключения для одной AI-функции.
 *
 * Связь intentionally per-function: наличие личного OpenAI/DeepSeek ключа не должно
 * автоматически менять провайдера всех AI-задач пользователя.
 */
@Table(name = "HUNTTECH_USER_AI_FUNCTION_OVERRIDE",
        uniqueConstraints = @UniqueConstraint(name = "UK_HUNTTECH_USER_AI_OVERRIDE_FUNCTION",
                columnNames = {"USER_ID", "AI_FUNCTION_ID"}),
        indexes = {
                @Index(name = "IDX_HUNTTECH_USER_AI_OVERRIDE_USER", columnList = "USER_ID"),
                @Index(name = "IDX_HUNTTECH_USER_AI_OVERRIDE_CONFIG", columnList = "USER_AI_CONFIGURATION_ID")
        })
@Entity(name = "hunttech_UserAiFunctionOverride")
@NamePattern("%s|aiFunction")
public class UserAiFunctionOverride extends StandardEntity {
    private static final long serialVersionUID = -8062394267296467404L;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AI_FUNCTION_ID", nullable = false)
    private AiFunctionConfiguration aiFunction;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_AI_CONFIGURATION_ID", nullable = false)
    private UserAiConfiguration userAiConfiguration;

    @Column(name = "MODEL_NAME", length = 128)
    private String modelName;

    @Column(name = "ENABLED")
    private Boolean enabled = true;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public AiFunctionConfiguration getAiFunction() {
        return aiFunction;
    }

    public void setAiFunction(AiFunctionConfiguration aiFunction) {
        this.aiFunction = aiFunction;
    }

    public UserAiConfiguration getUserAiConfiguration() {
        return userAiConfiguration;
    }

    public void setUserAiConfiguration(UserAiConfiguration userAiConfiguration) {
        this.userAiConfiguration = userAiConfiguration;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
