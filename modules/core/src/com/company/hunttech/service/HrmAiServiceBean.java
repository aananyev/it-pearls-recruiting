package com.company.hunttech.service;

import com.company.hunttech.core.ai.AIProvider;
import com.company.hunttech.core.ai.AIProviderRegistry;
import com.company.hunttech.core.ai.AiSecretService;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.ai.AiCapability;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.DataManager;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

/**
 * Совместимый vacancy AI-фасад поверх централизованного AI Control Plane.
 *
 * Рабочие методы принципиально не читают UserAiConfiguration/VacancyPromptTemplate
 * и не выбирают AIProvider напрямую. Это предотвращает обход per-function policy,
 * корпоративного fallback и централизованного управления prompt/model.
 */
@Service(HrmAiService.NAME)
public class HrmAiServiceBean implements HrmAiService {

    @Inject
    private AiExecutionService aiExecutionService;
    @Inject
    private AIProviderRegistry aiProviderRegistry;
    @Inject
    private AiSecretService aiSecretService;
    @Inject
    private DataManager dataManager;

    @Override
    public String standardizeVacancyDescription(String rawText) {
        Map<String, Object> ctx = new java.util.HashMap<>();
        ctx.put("rawDescription", rawText);
        ctx.put("callerSource", "HrmAiService (standardizeVacancyDescription)");
        return aiExecutionService.executeText(FUNCTION_STANDARDIZE_VACANCY, ctx).getText();
    }

    @Override
    public String generateChecklist(String vacancyText) {
        return generateVacancyArtifact(vacancyText, FUNCTION_VACANCY_CHECKLIST);
    }

    @Override
    public String generateSearchMap(String vacancyText) {
        return generateVacancyArtifact(vacancyText, FUNCTION_VACANCY_SEARCH_MAP);
    }

    @Override
    public String generateInterviewPlan(String vacancyText) {
        return generateVacancyArtifact(vacancyText, FUNCTION_VACANCY_INTERVIEW_PLAN);
    }

    @Override
    public String generateVacancyArtifact(String standardizedDescription, String functionCode) {
        Map<String, Object> ctx = new java.util.HashMap<>();
        ctx.put("description", standardizedDescription);
        ctx.put("callerSource", "HrmAiService (generateVacancyArtifact: " + functionCode + ")");
        return aiExecutionService.executeText(functionCode, ctx).getText();
    }

    /**
     * providerCode оставлен только в legacy API. Игнорирование намеренное: если
     * старый экран передаст выбранного vendor, он не сможет обойти function policy.
     */
    @Deprecated
    @Override
    public String standardizeVacancyDescription(String rawText, String providerCode) {
        return standardizeVacancyDescription(rawText);
    }

    /**
     * templateCode исторически был ключом VacancyPromptTemplate. После миграции
     * legacy templates получают AiFunctionConfiguration с тем же code, поэтому
     * старый вызов безопасно делегируется новому function-based контракту.
     */
    @Deprecated
    @Override
    public String generateVacancyArtifact(String standardizedDescription,
                                          String templateCode,
                                          String providerCode) {
        return generateVacancyArtifact(standardizedDescription, templateCode);
    }

    @Override
    public AiExecutionResult testConnection(UserAiConfiguration configuration) {
        /*
         * Диагностическая операция намеренно использует выбранную запись напрямую:
         * пользователь проверяет конкретные provider/key/model до назначения этого
         * credential на AI-функцию. Рабочая генерация этим путём не выполняется.
         */
        if (configuration == null) {
            throw new DevelopmentException("Не выбрана AI-конфигурация для тестирования.");
        }
        if (!isConfigured(configuration.getProviderCode())) {
            throw new DevelopmentException("Не указан провайдер AI.");
        }
        String apiKey = resolveApiKey(configuration);
        if (!isConfigured(apiKey)) {
            throw new DevelopmentException("Не указан API-ключ для провайдера «"
                    + configuration.getProviderCode() + "».");
        }

        AIProvider provider;
        try {
            provider = aiProviderRegistry.getProvider(configuration.getProviderCode());
        } catch (IllegalArgumentException e) {
            throw new DevelopmentException("Провайдер AI «"
                    + configuration.getProviderCode() + "» не подключён в приложении.", e);
        }

        String response;
        try {
            response = provider.generateText(
                    "Ответь одним словом: ok",
                    "Тестирование подключения к API искусственного интеллекта.",
                    apiKey,
                    configuration.getDefaultModelName(),
                    Map.of("temperature", 0.0));
        } catch (RuntimeException failure) {
            String safeMessage = AiSecuritySanitizer.sanitizeError(failure);
            throw new DevelopmentException(safeMessage == null
                    ? "Ошибка проверки AI-подключения."
                    : safeMessage);
        }

        if (!isConfigured(response)) {
            throw new DevelopmentException("API провайдера «"
                    + configuration.getProviderCode() + "» вернул пустой ответ.");
        }

        // Контракт пользовательской нотификации: диагностическая операция тоже несёт
        // метаданные AI-выполнения (модель, провайдер, собственник API = личный ключ
        // пользователя), чтобы экраны «Управление AI» показывали исчезающую нотификацию
        // «какая модель что делала + чей API» наравне с рабочими операциями.
        return AiExecutionResult.textResult(
                FUNCTION_TEST_CONNECTION,
                "Тестирование AI-подключения",
                AiCapability.TEXT_GENERATION,
                configuration.getDefaultModelName(),
                configuration.getProviderCode(),
                AiCredentialOwner.USER,
                response);
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String resolveApiKey(UserAiConfiguration configuration) {
        if (isConfigured(configuration.getApiKeyEncrypted())) {
            return aiSecretService.decrypt(configuration.getApiKeyEncrypted());
        }
        if (isConfigured(configuration.getApiKey())) {
            String plainText = configuration.getApiKey();
            configuration.setApiKeyEncrypted(aiSecretService.encrypt(plainText));
            configuration.setApiKey(null);
            dataManager.commit(configuration);
            return plainText;
        }
        return null;
    }
}
