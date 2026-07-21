package com.company.hunttech.service;

import com.company.hunttech.core.ai.AIProvider;
import com.company.hunttech.core.ai.AIProviderRegistry;
import com.haulmont.cuba.core.global.TemplateHelper;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.VacancyPromptTemplate;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

@Service(HrmAiService.NAME)
public class HrmAiServiceBean implements HrmAiService {

    private static final String STANDARDIZE_TEMPLATE_CODE = "STANDARDIZE_VACANCY";

    private static final String QUERY_USER_AI_CONFIG =
            "select e from hunttech_UserAiConfiguration e "
                    + "where e.user = :user and e.providerCode = :providerCode and e.isActive = true";

    private static final String QUERY_VACANCY_PROMPT_TEMPLATE =
            "select e from hunttech_VacancyPromptTemplate e where e.code = :code";

    @Inject
    private AIProviderRegistry aiProviderRegistry;
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSessionSource userSessionSource;

    @Override
    public String standardizeVacancyDescription(String rawText, String providerCode) {
        VacancyPromptTemplate template = loadVacancyPromptTemplate(STANDARDIZE_TEMPLATE_CODE);
        Map<String, Object> templateContext = Collections.singletonMap("rawDescription", rawText);
        String prompt = TemplateHelper.processTemplate(template.getPromptText(), templateContext);
        return callAiProvider(template, prompt, providerCode);
    }

    @Override
    public String generateVacancyArtifact(String standardizedDescription, String templateCode, String providerCode) {
        VacancyPromptTemplate template = loadVacancyPromptTemplate(templateCode);
        Map<String, Object> templateContext = Collections.singletonMap("description", standardizedDescription);
        String prompt = TemplateHelper.processTemplate(template.getPromptText(), templateContext);
        return callAiProvider(template, prompt, providerCode);
    }

    @Override
    public void testConnection(UserAiConfiguration configuration) {
        /*
         * Метод принимает конкретную UserAiConfiguration, а не только providerCode,
         * потому что личное окно настроек тестирует именно выбранную строку таблицы:
         * конкретный ключ, конкретную модель и конкретного провайдера. Рабочие
         * методы генерации ниже по-прежнему выбирают только активную конфигурацию
         * текущего пользователя через getUserConfig().
         */
        if (configuration == null) {
            throw new DevelopmentException("Не выбрана AI-конфигурация для тестирования.");
        }
        if (!isConfigured(configuration.getProviderCode())) {
            throw new DevelopmentException("Не указан провайдер AI.");
        }
        if (!isConfigured(configuration.getApiKey())) {
            throw new DevelopmentException("Не указан API-ключ для провайдера «"
                    + configuration.getProviderCode() + "».");
        }

        AIProvider provider;
        try {
            provider = aiProviderRegistry.getProvider(configuration.getProviderCode());
        } catch (IllegalArgumentException e) {
            /*
             * В интерфейсе могут быть коды провайдеров, запланированных на будущее.
             * Ошибку реестра переводим в понятное пользовательское сообщение:
             * Java-компонент провайдера пока не подключён к приложению.
             */
            throw new DevelopmentException("Провайдер AI «"
                    + configuration.getProviderCode() + "» не подключён в приложении.", e);
        }

        /*
         * Короткий детерминированный запрос делает тест недорогим, но при этом
         * проверяет авторизацию, доступность endpoint, имя выбранной модели и
         * разбор ответа внутри реализации провайдера.
         */
        String response = provider.generateText(
                "Ответь одним словом: ok",
                "Тестирование подключения к API искусственного интеллекта.",
                configuration.getApiKey(),
                configuration.getDefaultModelName(),
                Map.of("temperature", 0.0));

        if (!isConfigured(response)) {
            throw new DevelopmentException("API провайдера «"
                    + configuration.getProviderCode() + "» вернул пустой ответ.");
        }
    }

    @Override
    public String sendPrompt(String userPrompt, String providerCode) {
        if (!isConfigured(userPrompt)) {
            throw new DevelopmentException("Промпт не может быть пустым.");
        }
        if (!isConfigured(providerCode)) {
            throw new DevelopmentException("Не указан код провайдера AI.");
        }

        UserAiConfiguration config = getUserConfig(providerCode);
        AIProvider provider = aiProviderRegistry.getProvider(providerCode);

        return provider.generateText(
                userPrompt,
                "Ты — AI-ассистент рекрутинговой системы HRM HuntTech. "
                        + "Отвечай на русском языке развёрнуто и по делу.",
                config.getApiKey(),
                config.getDefaultModelName(),
                Map.of("temperature", 0.3));
    }

    private UserAiConfiguration getUserConfig(String providerCode) {
        User user = userSessionSource.getUserSession().getUser();
        UserAiConfiguration config = dataManager.load(UserAiConfiguration.class)
                .query(QUERY_USER_AI_CONFIG)
                .parameter("user", user)
                .parameter("providerCode", providerCode)
                .optional()
                .orElse(null);

        if (config == null || !isConfigured(config.getApiKey())) {
            throw new DevelopmentException(
                    "API-ключ для провайдера «" + providerCode + "» не настроен. "
                            + "Добавьте активную конфигурацию в настройках AI.");
        }
        return config;
    }

    private VacancyPromptTemplate loadVacancyPromptTemplate(String code) {
        return dataManager.load(VacancyPromptTemplate.class)
                .query(QUERY_VACANCY_PROMPT_TEMPLATE)
                .parameter("code", code)
                .optional()
                .orElseThrow(() -> new DevelopmentException(
                        "Шаблон промпта «" + code + "» не найден."));
    }

    private String callAiProvider(VacancyPromptTemplate template, String prompt, String providerCode) {
        UserAiConfiguration config = getUserConfig(providerCode);
        AIProvider provider = aiProviderRegistry.getProvider(providerCode);
        return provider.generateText(
                prompt,
                template.getSystemContext(),
                config.getApiKey(),
                config.getDefaultModelName(),
                buildOptions(template));
    }

    private Map<String, Object> buildOptions(VacancyPromptTemplate template) {
        Double temperature = template.getTemperature() != null ? template.getTemperature() : 0.7;
        return Map.of("temperature", temperature);
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
