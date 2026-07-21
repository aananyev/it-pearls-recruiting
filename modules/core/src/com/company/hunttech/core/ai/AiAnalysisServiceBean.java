package com.company.hunttech.core.ai;

import com.company.hunttech.entity.AiPromptTemplate;
import com.company.hunttech.service.AiAnalysisService;
import com.company.hunttech.service.HrmAiService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(AiAnalysisService.NAME)
public class AiAnalysisServiceBean implements AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisServiceBean.class);

    @Inject
    private DataManager dataManager;

    @Inject
    private HrmAiService hrmAiService;

    @Inject
    private EntityDataExtractors extractors;

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    @Override
    public String analyze(Entity entity, String promptCode) {
        // Сущность приходит из web-тира через CUBA remoting — она detached,
        // и её LAZY-связи потеряли EclipseLink Session. Перезагружаем в core-тире
        // через DataManager для восстановления работоспособности индирекции.
        Entity fullEntity = dataManager.load(entity.getClass())
                .id(entity.getId())
                .view(View.LOCAL)
                .one();
        log.info("Сущность перезагружена в core-тире: class={}, id={}",
                fullEntity.getClass().getSimpleName(), fullEntity.getId());

        log.info("Загружаем шаблон промпта: code={}", promptCode);

        AiPromptTemplate template = dataManager.load(AiPromptTemplate.class)
                .query("select e from hunttech_AiPromptTemplate e "
                        + "where e.code = :code and e.active = true")
                .parameter("code", promptCode)
                .view("_local")
                .optional()
                .orElseThrow(() -> {
                    log.error("Промпт не найден: code={}", promptCode);
                    return new DevelopmentException(
                            "Промпт с кодом «" + promptCode + "» не найден или неактивен.");
                });

        log.info("Шаблон загружен: name={}, entityClass={}, promptLength={}",
                template.getName(), template.getEntityClass(),
                template.getPromptText() != null ? template.getPromptText().length() : 0);

        String filledPrompt = fillPlaceholders(template.getPromptText(), fullEntity);
        log.debug("Промпт заполнен: length={}", filledPrompt.length());

        log.info("Отправляем промпт провайдеру openai (entity={}, promptCode={})",
                fullEntity.getClass().getSimpleName(), promptCode);

        try {
            String result = hrmAiService.sendPrompt(filledPrompt, "openai");
            log.info("Ответ получен: length={}", result != null ? result.length() : 0);
            return result;
        } catch (Exception e) {
            log.error("Ошибка вызова AI-провайдера: {}", e.toString(), e);
            throw e;
        }
    }

    private String fillPlaceholders(String template, Entity entity) {
        String result = template;
        Matcher m = PLACEHOLDER.matcher(template);
        while (m.find()) {
            String placeholder = m.group(1);
            String value = extractors.extract(entity, placeholder);
            if (value.startsWith("{{")) {
                log.warn("Placeholder не найден в реестре: entity={}, placeholder={}",
                        entity.getClass().getSimpleName(), placeholder);
            }
            result = result.replace("{{" + placeholder + "}}", value);
        }
        return result;
    }
}
