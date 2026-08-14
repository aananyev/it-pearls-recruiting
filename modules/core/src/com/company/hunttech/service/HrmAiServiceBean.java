package com.company.hunttech.service;

import com.company.hunttech.core.ai.AIProvider;
import com.company.hunttech.core.ai.AIProviderRegistry;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.VacancyPromptTemplate;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.TemplateHelper;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Совместимый vacancy AI-фасад поверх централизованного AI Control Plane.
 *
 * Рабочие методы принципиально не читают UserAiConfiguration/VacancyPromptTemplate
 * и не выбирают AIProvider напрямую. Это предотвращает обход per-function policy,
 * корпоративного fallback и централизованного управления prompt/model.
 */
@Service(HrmAiService.NAME)
public class HrmAiServiceBean implements HrmAiService {

    private static final Logger log = LoggerFactory.getLogger(HrmAiServiceBean.class);

    private static final String STANDARDIZE_TEMPLATE_CODE = "STANDARDIZE_VACANCY";

    private static final String QUERY_USER_AI_CONFIG =
            "select e from hunttech_UserAiConfiguration e "
                    + "where e.user = :user and e.providerCode = :providerCode";

    private static final String QUERY_CURRENT_AI_CONFIG =
            "select e from hunttech_UserAiConfiguration e "
                    + "where e.user = :user and e.isActive = true order by e.updateTs desc";

    private static final String QUERY_VACANCY_PROMPT_TEMPLATE =
            "select e from hunttech_VacancyPromptTemplate e where e.code = :code";

    @Inject
    private AIProviderRegistry aiProviderRegistry;
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Persistence persistence;

    @Override
    public String standardizeVacancyDescription(String rawText) {
        return aiExecutionService.executeText(
                FUNCTION_STANDARDIZE_VACANCY,
                Collections.<String, Object>singletonMap("rawDescription", rawText));
    }

    @Override
    public String generateVacancyArtifact(String standardizedDescription, String functionCode) {
        return aiExecutionService.executeText(
                functionCode,
                Collections.<String, Object>singletonMap("description", standardizedDescription));
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
    public void testConnection(UserAiConfiguration configuration) {
        /*
         * Метод принимает конкретную UserAiConfiguration, а не только providerCode,
         * потому что окно настроек тестирует именно выбранную строку: конкретный
         * ключ, модель и провайдера, независимо от того, назначена ли строка текущей.
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
            // В настройках могут храниться провайдеры, Java-компонент которых ещё не подключён.
            throw new DevelopmentException("Провайдер AI «"
                    + configuration.getProviderCode() + "» не подключён в приложении.", e);
        }

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
        if (!isConfigured(providerCode)) {
            throw new DevelopmentException("Не указан код провайдера AI.");
        }
        return sendPrompt(userPrompt, getUserConfig(providerCode));
    }

    @Override
    public String sendPromptUsingCurrentConfiguration(String userPrompt) {
        return sendPrompt(userPrompt, getCurrentUserConfig());
    }

    @Override
    public void setCurrentConfiguration(UUID configurationId) {
        if (configurationId == null) {
            throw new DevelopmentException("Не выбрана AI-конфигурация.");
        }

        /*
         * Снимаем текущий признак и назначаем новую конфигурацию в одной core-транзакции.
         * Это сохраняет инвариант «один пользователь — одна текущая нейросеть» даже
         * при наличии нескольких сохранённых API-ключей и моделей.
         */
        try (Transaction transaction = persistence.createTransaction()) {
            EntityManager entityManager = persistence.getEntityManager();
            UserAiConfiguration selected = entityManager.find(UserAiConfiguration.class, configurationId);
            if (selected == null) {
                throw new DevelopmentException("Выбранная AI-конфигурация не найдена.");
            }
            if (!isConfigured(selected.getProviderCode())) {
                throw new DevelopmentException("В выбранной AI-конфигурации не указан провайдер.");
            }
            if (!isConfigured(selected.getApiKey())) {
                throw new DevelopmentException("Для провайдера «" + selected.getProviderCode()
                        + "» не указан API-ключ.");
            }

            entityManager.createQuery(
                            "update hunttech_UserAiConfiguration e set e.isActive = false "
                                    + "where e.user = :user and e.id <> :configurationId and e.deleteTs is null")
                    .setParameter("user", selected.getUser())
                    .setParameter("configurationId", configurationId)
                    .executeUpdate();

            selected.setIsActive(true);
            transaction.commit();
        }
    }

    private String sendPrompt(String userPrompt, UserAiConfiguration configuration) {
        if (!isConfigured(userPrompt)) {
            log.error("sendPrompt отклонён: пустой промпт");
            throw new DevelopmentException("Промпт не может быть пустым.");
        }

        String providerCode = configuration.getProviderCode();
        log.info("Отправка AI-промпта: provider={}, promptLength={}, model={}",
                providerCode, userPrompt.length(), configuration.getDefaultModelName());

        AIProvider provider = aiProviderRegistry.getProvider(providerCode);
        try {
            String response = provider.generateText(
                    userPrompt,
                    "Ты — AI-ассистент рекрутинговой системы HRM HuntTech. "
                            + "Отвечай на русском языке развёрнуто и по делу.",
                    configuration.getApiKey(),
                    configuration.getDefaultModelName(),
                    Map.of("temperature", 0.3));

            log.info("Ответ получен от {}: responseLength={}", providerCode,
                    response != null ? response.length() : 0);
            return response;
        } catch (Exception e) {
            log.error("Ошибка при вызове {}.generateText(): {}",
                    provider.getClass().getSimpleName(), e.toString(), e);
            throw e;
        }
    }

    /**
     * Загружает сохранённую конфигурацию явно выбранного провайдера.
     * Признак isActive здесь не используется: он зарезервирован для единственной
     * текущей нейросети системного AI-анализа, а остальные настройки можно тестировать
     * и использовать в сценариях с явным выбором провайдера.
     */
    private UserAiConfiguration getUserConfig(String providerCode) {
        User user = userSessionSource.getUserSession().getUser();
        UserAiConfiguration config = dataManager.load(UserAiConfiguration.class)
                .query(QUERY_USER_AI_CONFIG)
                .parameter("user", user)
                .parameter("providerCode", providerCode)
                .view("userAiConfiguration-edit-view")
                .optional()
                .orElse(null);

        if (config == null || !isConfigured(config.getApiKey())) {
            throw new DevelopmentException(
                    "API-ключ для провайдера «" + providerCode + "» не настроен. "
                            + "Добавьте конфигурацию в настройках AI.");
        }
        return config;
    }

    private UserAiConfiguration getCurrentUserConfig() {
        User user = userSessionSource.getUserSession().getUser();
        List<UserAiConfiguration> currentConfigurations = dataManager.load(UserAiConfiguration.class)
                .query(QUERY_CURRENT_AI_CONFIG)
                .parameter("user", user)
                .view("userAiConfiguration-edit-view")
                .maxResults(2)
                .list();

        if (currentConfigurations.isEmpty()) {
            throw new DevelopmentException(
                    "Не выбрана текущая нейросеть для AI-анализа. "
                            + "Администратор должен выбрать её в настройках доступа к AI API.");
        }
        if (currentConfigurations.size() > 1) {
            throw new DevelopmentException(
                    "Для пользователя выбрано несколько текущих AI-конфигураций. "
                            + "Оставьте текущей только одну нейросеть.");
        }

        UserAiConfiguration config = currentConfigurations.get(0);
        if (!isConfigured(config.getProviderCode())) {
            throw new DevelopmentException("В текущей AI-конфигурации не указан провайдер.");
        }
        if (!isConfigured(config.getApiKey())) {
            throw new DevelopmentException("API-ключ текущего провайдера «"
                    + config.getProviderCode() + "» не настроен.");
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
