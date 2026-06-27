package com.company.itpearls.service;

import com.company.itpearls.core.ai.AIProvider;
import com.company.itpearls.core.ai.AIProviderRegistry;
import com.haulmont.cuba.core.global.TemplateHelper;
import com.company.itpearls.entity.UserAiConfiguration;
import com.company.itpearls.entity.VacancyPromptTemplate;
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
            "select e from itpearls_UserAiConfiguration e "
                    + "where e.user = :user and e.providerCode = :providerCode and e.isActive = true";

    private static final String QUERY_VACANCY_PROMPT_TEMPLATE =
            "select e from itpearls_VacancyPromptTemplate e where e.code = :code";

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
