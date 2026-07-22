package com.company.hunttech.core.ai;

import com.company.hunttech.entity.AiPromptTemplate;
import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.service.AiAnalysisService;
import com.company.hunttech.service.HrmAiService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.UUID;
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
        // LAZY-связи потеряли EclipseLink Session. Перезагружаем в core-тире
        // со специализированным view: View.LOCAL недостаточен для LAZY-связей.
        Entity fullEntity = reloadWithAnalysisView(entity);
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

        // Системные кнопки AI-анализа всегда используют одну текущую конфигурацию пользователя,
        // выбранную администратором в списке настроек доступа к AI API.
        log.info("Отправляем системный промпт через текущую AI-конфигурацию (entity={}, promptCode={})",
                fullEntity.getClass().getSimpleName(), promptCode);

        try {
            String result = hrmAiService.sendPromptUsingCurrentConfiguration(filledPrompt);
            log.info("Ответ получен: length={}", result != null ? result.length() : 0);
            return result;
        } catch (Exception e) {
            log.error("Ошибка вызова AI-провайдера: {}", e.toString(), e);
            throw e;
        }
    }

    /**
     * Перезагружает сущность в core-тире со специализированным view,
     * включающим LAZY-связи, необходимые экстракторам placeholder-ов.
     * View.LOCAL загружает только прямые поля, но НЕ ManyToOne-связи,
     * которые экстракторы читают (CandidateCV.candidate, OpenPosition.projectName и др.).
     */
    private Entity reloadWithAnalysisView(Entity entity) {
        Class<? extends Entity> cls = entity.getClass();
        // Убираем Hibernate-прокси-суффикс
        String simpleName = cls.getSimpleName().replaceAll("\\$.*", "");

        UUID entityId = (UUID) entity.getId();
        switch (simpleName) {
            case "CandidateCV":
                return dataManager.load(CandidateCV.class)
                        .id(entityId)
                        .view(buildCandidateCVAnalysisView())
                        .one();
            case "OpenPosition":
                return dataManager.load(OpenPosition.class)
                        .id(entityId)
                        .view(buildOpenPositionAnalysisView())
                        .one();
            case "IteractionList":
                return dataManager.load(IteractionList.class)
                        .id(entityId)
                        .view(buildIteractionListAnalysisView())
                        .one();
            case "JobCandidate":
                return dataManager.load(JobCandidate.class)
                        .id(entityId)
                        .view(buildJobCandidateAnalysisView())
                        .one();
            default:
                throw new DevelopmentException("Нет analysis-view для класса «"
                        + simpleName + "».");
        }
    }

    private View buildCandidateCVAnalysisView() {
        return ViewBuilder.of(CandidateCV.class)
                .addAll("textCV", "candidate", "toVacancy")
                .addView(ViewBuilder.of(OpenPosition.class)
                        .addAll("vacansyName", "shortDescription")
                        .build())
                .addView(ViewBuilder.of(JobCandidate.class)
                        .addView("_minimal")
                        .build())
                .build();
    }

    private View buildOpenPositionAnalysisView() {
        // Каждый вложенный view привязывается к конкретному property родительской сущности.
        // Перегрузка addView(View) не определяет автоматически связь Project → Department → Company.
        return ViewBuilder.of(OpenPosition.class)
                .addAll("shortDescription", "comment")
                .addView("projectName",
                        ViewBuilder.of(com.company.hunttech.entity.Project.class)
                                .addAll("projectName")
                                .addView("projectDepartment",
                                        ViewBuilder.of(com.company.hunttech.entity.CompanyDepartament.class)
                                                .addView("companyName",
                                                        ViewBuilder.of(com.company.hunttech.entity.Company.class)
                                                                .addAll("comanyName")
                                                                .build())
                                                .build())
                                .build())
                .build();
    }

    private View buildIteractionListAnalysisView() {
        return ViewBuilder.of(IteractionList.class)
                .addAll("comment", "dateIteraction", "recrutierName",
                        "iteractionType", "candidate")
                .addView(ViewBuilder.of(com.company.hunttech.entity.Iteraction.class)
                        .addAll("iterationName")
                        .build())
                .addView(ViewBuilder.of(JobCandidate.class)
                        .addView("_minimal")
                        .build())
                .build();
    }

    private View buildJobCandidateAnalysisView() {
        return ViewBuilder.of(JobCandidate.class)
                .addAll("email", "phone")
                .addView("_minimal")
                .build();
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
