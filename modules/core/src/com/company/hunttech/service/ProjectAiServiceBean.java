package com.company.hunttech.service;

import com.haulmont.cuba.core.global.DevelopmentException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI-фасад ProjectEdit поверх общего AiExecutionService.
 *
 * Здесь намеренно нет prompt/provider/model/API key: эти параметры принадлежат
 * AiFunctionConfiguration и изменяются администратором без выпуска нового кода.
 */
@Service(ProjectAiService.NAME)
public class ProjectAiServiceBean implements ProjectAiService {
    private static final int MAX_SOURCE_TEXT_LENGTH = 120_000;

    @Inject
    private AiExecutionService aiExecutionService;

    @Override
    public String processUploadedDescription(String projectName,
                                             String sourceFileName,
                                             String sourceText) {
        if (!isConfigured(sourceText)) {
            throw new DevelopmentException("Загруженное описание проекта не содержит текста.");
        }
        String normalizedSource = sourceText.trim();
        if (normalizedSource.length() > MAX_SOURCE_TEXT_LENGTH) {
            throw new DevelopmentException(
                    "Текст описания проекта слишком большой для автоматической AI-обработки.");
        }

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("projectName", safeValue(projectName));
        context.put("sourceFileName", safeValue(sourceFileName));
        context.put("sourceText", normalizedSource);

        String result = aiExecutionService.executeText(
                FUNCTION_PROJECT_DESCRIPTION_GENERATE, context);
        if (!isConfigured(result)) {
            throw new DevelopmentException("AI вернул пустое описание проекта.");
        }
        return result.trim();
    }

    @Override
    public String generateShortDescription(String projectName, String descriptionText) {
        if (!isConfigured(descriptionText)) {
            throw new DevelopmentException(
                    "Описание проекта пусто — краткое описание не может быть сгенерировано.");
        }
        String normalizedSource = descriptionText.trim();
        if (normalizedSource.length() > MAX_SOURCE_TEXT_LENGTH) {
            throw new DevelopmentException(
                    "Текст описания проекта слишком большой для автоматической AI-обработки.");
        }

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("projectName", safeValue(projectName));
        context.put("sourceText", normalizedSource);

        String result = aiExecutionService.executeText(
                FUNCTION_PROJECT_SHORT_DESCRIPTION_GENERATE, context);
        if (!isConfigured(result)) {
            throw new DevelopmentException("AI вернул пустое краткое описание проекта.");
        }
        return result.trim();
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
