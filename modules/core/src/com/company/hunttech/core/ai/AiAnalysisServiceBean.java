package com.company.hunttech.core.ai;

import com.company.hunttech.entity.AiPromptTemplate;
import com.company.hunttech.service.AiAnalysisService;
import com.company.hunttech.service.HrmAiService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Реализация сервиса AI-анализа сущностей.
 *
 * Алгоритм:
 * 1. Загружает активный AiPromptTemplate по коду.
 * 2. Заполняет {{placeholders}} данными сущности через EntityDataExtractors.
 * 3. Отправляет заполненный промпт провайдеру AI через HrmAiService.sendPrompt().
 */
@Service(AiAnalysisService.NAME)
public class AiAnalysisServiceBean implements AiAnalysisService {

    @Inject
    private DataManager dataManager;

    @Inject
    private HrmAiService hrmAiService;

    @Inject
    private EntityDataExtractors extractors;

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    @Override
    public String analyze(Entity entity, String promptCode) {
        // 1. Загружаем активный шаблон промпта
        AiPromptTemplate template = dataManager.load(AiPromptTemplate.class)
                .query("select e from hunttech_AiPromptTemplate e "
                        + "where e.code = :code and e.active = true")
                .parameter("code", promptCode)
                .view("_local")
                .optional()
                .orElseThrow(() -> new DevelopmentException(
                        "Промпт с кодом «" + promptCode + "» не найден или неактивен."));

        // 2. Заполняем {{placeholders}} данными сущности
        String filledPrompt = fillPlaceholders(template.getPromptText(), entity);

        // 3. Отправляем AI-провайдеру (пока — openai по умолчанию,
        //    в будущем через UserAiConfiguration пользователя)
        return hrmAiService.sendPrompt(filledPrompt, "openai");
    }

    private String fillPlaceholders(String template, Entity entity) {
        String result = template;
        Matcher m = PLACEHOLDER.matcher(template);
        while (m.find()) {
            String placeholder = m.group(1);
            String value = extractors.extract(entity, placeholder);
            result = result.replace("{{" + placeholder + "}}", value);
        }
        return result;
    }
}
