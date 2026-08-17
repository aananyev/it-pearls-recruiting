package com.company.hunttech.service;

import com.company.hunttech.core.ai.AIProvider;
import com.company.hunttech.core.ai.AIProviderRegistry;
import com.company.hunttech.core.ai.AiCostCalculator;
import com.company.hunttech.core.ai.AiProviderResponse;
import com.company.hunttech.core.ai.AiSecretService;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.company.hunttech.entity.ai.AiCallLog;
import com.company.hunttech.entity.ai.AiCapability;
import com.company.hunttech.entity.ai.AiExecutionPolicy;
import com.company.hunttech.entity.ai.AiFallbackPolicy;
import com.company.hunttech.entity.ai.AiFunctionConfiguration;
import com.company.hunttech.entity.ai.UserAiFunctionOverride;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.TemplateHelper;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Централизованный resolver и execution layer для AI-функций HRM HuntTech.
 *
 * Выбор credential выполняется строго по policy функции. Пользовательский ключ может
 * заместить корпоративный только через UserAiFunctionOverride для конкретной функции.
 * Все вызовы фиксируются в сущности AiCallLog с токенами, длительностью, промптом и статусом.
 */
@Service(AiExecutionService.NAME)
public class AiExecutionServiceBean implements AiExecutionService {
    private static final Logger log = LoggerFactory.getLogger(AiExecutionServiceBean.class);

    private static final String QUERY_FUNCTION =
            "select e from hunttech_AiFunctionConfiguration e where e.code = :code and e.active = true";
    private static final String QUERY_OVERRIDE =
            "select e from hunttech_UserAiFunctionOverride e "
                    + "where e.user = :user and e.aiFunction = :function and e.enabled = true";

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private AIProviderRegistry aiProviderRegistry;
    @Inject
    private AiSecretService aiSecretService;

    @Override
    public AiExecutionResult executeText(String functionCode, Map<String, Object> context) {
        long startTime = System.currentTimeMillis();
        String callerSource = context != null && context.get("callerSource") != null
                ? String.valueOf(context.get("callerSource")) : null;

        AiFunctionConfiguration function = loadFunction(functionCode);
        validateTextCapability(function);
        String prompt = buildPrompt(function, context == null ? Collections.emptyMap() : context);
        User currentUser = userSessionSource.getUserSession().getUser();
        AiExecutionPolicy policy = function.getExecutionPolicy();
        if (policy == null) {
            throw new DevelopmentException("Для AI-функции «" + functionCode + "» не задана политика выполнения.");
        }

        UserAiFunctionOverride userOverride = loadUserOverride(currentUser, function);
        if (AiExecutionPolicy.USER_REQUIRED == policy) {
            validateUserOverride(userOverride, currentUser, functionCode);
            return executeWithUser(function, userOverride, prompt, currentUser, callerSource, startTime);
        }
        if (AiExecutionPolicy.USER_OVERRIDE_ALLOWED == policy && isUsableUserOverride(userOverride, currentUser)) {
            try {
                return executeWithUser(function, userOverride, prompt, currentUser, callerSource, startTime);
            } catch (RuntimeException userFailure) {
                if (AiFallbackPolicy.FALLBACK_TO_ADMIN == function.getFallbackPolicy()
                        && isUsableAdminConfiguration(function.getAdminConfiguration())) {
                    log.warn("Персональное AI-подключение функции {} недоступно; используется разрешённый admin fallback. Причина: {}",
                            functionCode, userFailure.getClass().getSimpleName());
                    return executeWithAdmin(function, prompt, currentUser, callerSource, startTime);
                }
                saveAiCallLog(currentUser, function, null, null, "USER", prompt, null,
                        null, null, null, System.currentTimeMillis() - startTime, callerSource, "ERROR", userFailure.getMessage());
                throw new DevelopmentException(
                        "Персональное AI-подключение для функции «" + functionCode + "» недоступно.", userFailure);
            }
        }
        return executeWithAdmin(function, prompt, currentUser, callerSource, startTime);
    }

    @Override
    public AiExecutionResult executeImage(String functionCode, Map<String, Object> context,
                                          byte[] sourceImage, String sourceMimeType) {
        long startTime = System.currentTimeMillis();
        String callerSource = context != null && context.get("callerSource") != null
                ? String.valueOf(context.get("callerSource")) : null;

        if (sourceImage == null || sourceImage.length == 0) {
            throw new DevelopmentException("Для AI-обработки изображения не переданы данные.");
        }
        AiFunctionConfiguration function = loadFunction(functionCode);
        validateImageCapability(function);
        String prompt = buildPrompt(function, context == null ? Collections.emptyMap() : context);
        User currentUser = userSessionSource.getUserSession().getUser();
        AiExecutionPolicy policy = function.getExecutionPolicy();
        if (policy == null) {
            throw new DevelopmentException("Для AI-функции «" + functionCode + "» не задана политика выполнения.");
        }

        UserAiFunctionOverride userOverride = loadUserOverride(currentUser, function);
        if (AiExecutionPolicy.USER_REQUIRED == policy) {
            validateUserOverride(userOverride, currentUser, functionCode);
            return executeWithUserImage(function, userOverride, prompt, sourceImage, sourceMimeType, currentUser, callerSource, startTime);
        }
        if (AiExecutionPolicy.USER_OVERRIDE_ALLOWED == policy && isUsableUserOverride(userOverride, currentUser)) {
            try {
                return executeWithUserImage(function, userOverride, prompt, sourceImage, sourceMimeType, currentUser, callerSource, startTime);
            } catch (RuntimeException userFailure) {
                if (AiFallbackPolicy.FALLBACK_TO_ADMIN == function.getFallbackPolicy()
                        && isUsableAdminConfiguration(function.getAdminConfiguration())) {
                    log.warn("Персональное AI-подключение функции {} недоступно; используется разрешённый admin fallback. Причина: {}",
                            functionCode, userFailure.getClass().getSimpleName());
                    return executeWithAdminImage(function, prompt, sourceImage, sourceMimeType, currentUser, callerSource, startTime);
                }
                saveAiCallLog(currentUser, function, null, null, "USER", prompt, null,
                        null, null, null, System.currentTimeMillis() - startTime, callerSource, "ERROR", userFailure.getMessage());
                throw new DevelopmentException(
                        "Персональное AI-подключение для функции «" + functionCode + "» недоступно.", userFailure);
            }
        }
        return executeWithAdminImage(function, prompt, sourceImage, sourceMimeType, currentUser, callerSource, startTime);
    }

    private AiExecutionResult executeWithUserImage(AiFunctionConfiguration function,
                                                   UserAiFunctionOverride override,
                                                   String prompt, byte[] sourceImage, String sourceMimeType,
                                                   User currentUser, String callerSource, long startTime) {
        UserAiConfiguration configuration = override.getUserAiConfiguration();
        String model = configuration.getDefaultModelName();
        if (Boolean.TRUE.equals(function.getAllowModelOverride()) && isConfigured(override.getModelName())) {
            model = override.getModelName();
        }
        try {
            byte[] image = executeProviderImage(configuration.getProviderCode(), configuration.getApiKey(), model,
                    function, prompt, sourceImage, sourceMimeType);
            saveAiCallLog(currentUser, function, configuration.getProviderCode(), model, AiCredentialOwner.USER.name(),
                    prompt, "[IMAGE DATA " + (image != null ? image.length : 0) + " bytes]",
                    null, null, null, System.currentTimeMillis() - startTime, callerSource, "SUCCESS", null);
            return AiExecutionResult.imageResult(function.getCode(), function.getName(), function.getCapability(),
                    model, configuration.getProviderCode(), AiCredentialOwner.USER, image);
        } catch (Exception e) {
            saveAiCallLog(currentUser, function, configuration.getProviderCode(), model, AiCredentialOwner.USER.name(),
                    prompt, null, null, null, null, System.currentTimeMillis() - startTime, callerSource, "ERROR", e.getMessage());
            throw e;
        }
    }

    private AiExecutionResult executeWithAdminImage(AiFunctionConfiguration function, String prompt,
                                                    byte[] sourceImage, String sourceMimeType,
                                                    User currentUser, String callerSource, long startTime) {
        AdminAiConfiguration configuration = function.getAdminConfiguration();
        if (!isUsableAdminConfiguration(configuration)) {
            throw new DevelopmentException(
                    "Для AI-функции «" + function.getCode() + "» не настроено активное корпоративное подключение.");
        }
        String model = isConfigured(function.getAdminModelName())
                ? function.getAdminModelName() : configuration.getDefaultModelName();
        String apiKey = aiSecretService.decrypt(configuration.getApiKeyEncrypted());
        try {
            byte[] image = executeProviderImage(configuration.getProviderCode(), apiKey, model, function,
                    prompt, sourceImage, sourceMimeType);
            saveAiCallLog(currentUser, function, configuration.getProviderCode(), model, AiCredentialOwner.ADMIN.name(),
                    prompt, "[IMAGE DATA " + (image != null ? image.length : 0) + " bytes]",
                    null, null, null, System.currentTimeMillis() - startTime, callerSource, "SUCCESS", null);
            return AiExecutionResult.imageResult(function.getCode(), function.getName(), function.getCapability(),
                    model, configuration.getProviderCode(), AiCredentialOwner.ADMIN, image);
        } catch (Exception e) {
            saveAiCallLog(currentUser, function, configuration.getProviderCode(), model, AiCredentialOwner.ADMIN.name(),
                    prompt, null, null, null, null, System.currentTimeMillis() - startTime, callerSource, "ERROR", e.getMessage());
            throw e;
        }
    }

    private AiExecutionResult executeWithUser(AiFunctionConfiguration function,
                                              UserAiFunctionOverride override,
                                              String prompt,
                                              User currentUser, String callerSource, long startTime) {
        UserAiConfiguration configuration = override.getUserAiConfiguration();
        String model = configuration.getDefaultModelName();
        if (Boolean.TRUE.equals(function.getAllowModelOverride()) && isConfigured(override.getModelName())) {
            model = override.getModelName();
        }
        try {
            AiProviderResponse response = executeProvider(configuration.getProviderCode(), configuration.getApiKey(), model,
                    function, prompt);
            saveAiCallLog(currentUser, function, configuration.getProviderCode(), model, AiCredentialOwner.USER.name(),
                    prompt, response.getText(), response.getPromptTokens(), response.getCompletionTokens(),
                    response.getTotalTokens(), System.currentTimeMillis() - startTime, callerSource, "SUCCESS", null);
            return AiExecutionResult.textResult(function.getCode(), function.getName(), function.getCapability(),
                    model, configuration.getProviderCode(), AiCredentialOwner.USER, response.getText());
        } catch (Exception e) {
            saveAiCallLog(currentUser, function, configuration.getProviderCode(), model, AiCredentialOwner.USER.name(),
                    prompt, null, null, null, null, System.currentTimeMillis() - startTime, callerSource, "ERROR", e.getMessage());
            throw e;
        }
    }

    private AiExecutionResult executeWithAdmin(AiFunctionConfiguration function, String prompt,
                                               User currentUser, String callerSource, long startTime) {
        AdminAiConfiguration configuration = function.getAdminConfiguration();
        if (!isUsableAdminConfiguration(configuration)) {
            throw new DevelopmentException(
                    "Для AI-функции «" + function.getCode() + "» не настроено активное корпоративное подключение.");
        }
        String model = isConfigured(function.getAdminModelName())
                ? function.getAdminModelName() : configuration.getDefaultModelName();
        String apiKey = aiSecretService.decrypt(configuration.getApiKeyEncrypted());
        try {
            AiProviderResponse response = executeProvider(configuration.getProviderCode(), apiKey, model, function, prompt);
            saveAiCallLog(currentUser, function, configuration.getProviderCode(), model, AiCredentialOwner.ADMIN.name(),
                    prompt, response.getText(), response.getPromptTokens(), response.getCompletionTokens(),
                    response.getTotalTokens(), System.currentTimeMillis() - startTime, callerSource, "SUCCESS", null);
            return AiExecutionResult.textResult(function.getCode(), function.getName(), function.getCapability(),
                    model, configuration.getProviderCode(), AiCredentialOwner.ADMIN, response.getText());
        } catch (Exception e) {
            saveAiCallLog(currentUser, function, configuration.getProviderCode(), model, AiCredentialOwner.ADMIN.name(),
                    prompt, null, null, null, null, System.currentTimeMillis() - startTime, callerSource, "ERROR", e.getMessage());
            throw e;
        }
    }

    private AiProviderResponse executeProvider(String providerCode,
                                               String apiKey,
                                               String model,
                                               AiFunctionConfiguration function,
                                               String prompt) {
        if (!isConfigured(providerCode) || !isConfigured(apiKey)) {
            throw new DevelopmentException("Эффективное AI-подключение настроено не полностью.");
        }
        AIProvider provider;
        try {
            provider = aiProviderRegistry.getProvider(providerCode);
        } catch (IllegalArgumentException e) {
            throw new DevelopmentException("Провайдер AI «" + providerCode + "» не подключён в приложении.", e);
        }
        return provider.executeTextWithTokens(prompt, function.getSystemPrompt(), apiKey, model, buildOptions(function));
    }

    private void saveAiCallLog(User user, AiFunctionConfiguration function, String providerCode,
                               String modelName, String credentialOwner, String prompt,
                               String responseText, Integer promptTokens, Integer completionTokens,
                               Integer totalTokens, Long durationMs, String callerSource,
                               String status, String errorMessage) {
        try {
            AiCallLog callLog = metadata.create(AiCallLog.class);
            callLog.setUser(user);
            if (user != null) {
                callLog.setUserLogin(user.getLogin());
                callLog.setUserName(user.getName());
            }
            callLog.setCallTime(new Date());
            callLog.setDurationMs(durationMs);
            if (function != null) {
                callLog.setFunctionCode(function.getCode());
                callLog.setFunctionName(function.getName());
                callLog.setCapability(function.getCapability() != null ? function.getCapability().name() : null);
            }
            callLog.setProviderCode(providerCode);
            callLog.setModelName(modelName);
            callLog.setCredentialOwner(credentialOwner);
            callLog.setPromptTokens(promptTokens);
            callLog.setCompletionTokens(completionTokens);
            callLog.setTotalTokens(totalTokens);

            AiCostCalculator.CostResult costResult = AiCostCalculator.calculateCost(providerCode, modelName, promptTokens, completionTokens);
            callLog.setEstimatedCost(costResult.getCost());
            callLog.setCurrency(costResult.getCurrency());

            callLog.setPromptText(prompt);
            callLog.setResponseText(responseText);
            callLog.setCallerSource(callerSource);
            callLog.setStatus(status);
            callLog.setErrorMessage(errorMessage);

            dataManager.commit(new CommitContext(callLog));
        } catch (Exception e) {
            log.error("Не удалось сохранить запись журнала вызовов AI: {}", e.getMessage(), e);
        }
    }

    private byte[] executeProviderImage(String providerCode, String apiKey, String model,
                                        AiFunctionConfiguration function, String prompt,
                                        byte[] sourceImage, String sourceMimeType) {
        if (!isConfigured(providerCode) || !isConfigured(apiKey)) {
            throw new DevelopmentException("Эффективное AI-подключение настроено не полностью.");
        }
        AIProvider provider;
        try {
            provider = aiProviderRegistry.getProvider(providerCode);
        } catch (IllegalArgumentException e) {
            throw new DevelopmentException("Провайдер AI «" + providerCode + "» не подключён в приложении.", e);
        }
        return provider.generateImage(prompt, function.getSystemPrompt(), apiKey, model,
                buildOptions(function), sourceImage, sourceMimeType);
    }

    private void validateImageCapability(AiFunctionConfiguration function) {
        AiCapability capability = function.getCapability();
        if (AiCapability.IMAGE_GENERATION != capability) {
            throw new DevelopmentException(
                    "AI-функция «" + function.getCode() + "» требует capability «" + capability
                            + "», которая не поддержана image execution layer (ожидается IMAGE_GENERATION).");
        }
    }

    private AiFunctionConfiguration loadFunction(String functionCode) {
        if (!isConfigured(functionCode)) {
            throw new DevelopmentException("Не задан код AI-функции.");
        }
        return dataManager.load(AiFunctionConfiguration.class)
                .query(QUERY_FUNCTION)
                .parameter("code", functionCode)
                .view("ai-function-execution-view")
                .optional()
                .orElseThrow(() -> new DevelopmentException(
                        "Активная AI-функция «" + functionCode + "» не найдена."));
    }

    private UserAiFunctionOverride loadUserOverride(User user, AiFunctionConfiguration function) {
        return dataManager.load(UserAiFunctionOverride.class)
                .query(QUERY_OVERRIDE)
                .parameter("user", user)
                .parameter("function", function)
                .view("user-ai-function-override-execution-view")
                .optional()
                .orElse(null);
    }

    private String buildPrompt(AiFunctionConfiguration function, Map<String, Object> context) {
        if (!isConfigured(function.getPromptTemplate())) {
            throw new DevelopmentException("Для AI-функции «" + function.getCode() + "» не задан prompt template.");
        }
        return TemplateHelper.processTemplate(function.getPromptTemplate(), context);
    }

    private Map<String, Object> buildOptions(AiFunctionConfiguration function) {
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", function.getTemperature() == null ? 0.7 : function.getTemperature());
        if (function.getMaxTokens() != null) {
            options.put("maxTokens", function.getMaxTokens());
        }
        return options;
    }

    private void validateTextCapability(AiFunctionConfiguration function) {
        AiCapability capability = function.getCapability();
        boolean supported = AiCapability.TEXT_GENERATION == capability
                || AiCapability.TEXT_ANALYSIS == capability
                || AiCapability.TEXT_TRANSFORMATION == capability
                || AiCapability.DOCUMENT_ANALYSIS == capability;
        if (!supported) {
            throw new DevelopmentException(
                    "AI-функция «" + function.getCode() + "» требует capability «" + capability
                            + "», которая ещё не поддержана текстовым execution layer.");
        }
    }

    private void validateUserOverride(UserAiFunctionOverride override, User currentUser, String functionCode) {
        if (!isUsableUserOverride(override, currentUser)) {
            throw new DevelopmentException(
                    "Для AI-функции «" + functionCode + "» требуется активное персональное подключение.");
        }
    }

    private boolean isUsableUserOverride(UserAiFunctionOverride override, User currentUser) {
        if (override == null || !Boolean.TRUE.equals(override.getEnabled())) {
            return false;
        }
        UserAiConfiguration configuration = override.getUserAiConfiguration();
        return configuration != null
                && configuration.getUser() != null
                && configuration.getUser().getId().equals(currentUser.getId())
                && Boolean.TRUE.equals(configuration.getIsActive())
                && isConfigured(configuration.getProviderCode())
                && isConfigured(configuration.getApiKey());
    }

    private boolean isUsableAdminConfiguration(AdminAiConfiguration configuration) {
        return configuration != null
                && Boolean.TRUE.equals(configuration.getActive())
                && isConfigured(configuration.getProviderCode())
                && isConfigured(configuration.getApiKeyEncrypted());
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
