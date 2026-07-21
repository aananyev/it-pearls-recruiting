package com.company.hunttech.web.screens.aiprompttemplate;

import com.company.hunttech.entity.AiPromptTemplate;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("hunttech_AiPromptTemplate.browse")
@UiDescriptor("ai-prompt-template-browse.xml")
@LoadDataBeforeShow
public class AiPromptTemplateBrowse extends StandardLookup<AiPromptTemplate> {

    @Inject
    private DataManager dataManager;

    @Subscribe
    public void onInit(InitEvent event) {
        // Проверяем через скалярный запрос — не вызывает материализацию сущности
        Long count = dataManager.loadValues("select count(e) from hunttech_AiPromptTemplate e")
                .properties("count")
                .list()
                .stream()
                .findFirst()
                .map(kv -> kv.<Long>getValue("count"))
                .orElse(0L);

        if (count == 0) {
            seedPrompt("RESUME_ANALYSIS", "Анализ резюме",
                    "com.company.hunttech.entity.CandidateCV",
                    "Ты — опытный рекрутер. Проанализируй резюме.\n\n"
                    + "Кандидат: {{candidateName}}\nВакансия: {{positionName}}\n\n"
                    + "=== РЕЗЮМЕ ===\n{{resumeText}}\n\n=== ВАКАНСИЯ ===\n{{vacancyDescription}}\n\n"
                    + "Оцени по шкале 1-10. Сильные/слабые стороны. Рекомендации.",
                    "{\"resumeText\":\"текст\",\"candidateName\":\"ФИО\",\"vacancyDescription\":\"описание\",\"positionName\":\"позиция\"}");

            seedPrompt("VACANCY_ANALYSIS", "Расшифровка вакансии",
                    "com.company.hunttech.entity.OpenPosition",
                    "Ты — AI-ассистент рекрутера. Объясни вакансию.\n\n"
                    + "Компания: {{companyName}}\nПроект: {{projectName}}\n"
                    + "Описание: {{vacancyDescription}}\nТребования: {{vacancyRequirements}}\n\n"
                    + "Опиши суть, ключевые навыки, портрет кандидата, рекомендации.",
                    "{\"vacancyDescription\":\"описание\",\"vacancyRequirements\":\"требования\",\"companyName\":\"компания\",\"projectName\":\"проект\"}");

            seedPrompt("INTERACTION_ANALYSIS", "Анализ воронки",
                    "com.company.hunttech.entity.IteractionList",
                    "Ты — AI-ассистент рекрутера. Проанализируй воронку.\n\n"
                    + "Тип: {{interactionType}} | Дата: {{dateIteraction}} | Рекрутер: {{recrutierName}}\n"
                    + "Комментарий: {{comment}}\nКандидат: {{candidateName}}\n\n"
                    + "=== ИСТОРИЯ ===\n{{candidateHistory}}\n\n"
                    + "Стадия воронки, признаки зависания, прогноз, рекомендация.",
                    "{\"interactionType\":\"тип\",\"dateIteraction\":\"дата\",\"recrutierName\":\"рекрутер\",\"comment\":\"комментарий\",\"candidateName\":\"ФИО\",\"candidateHistory\":\"история\"}");
        }
    }

    private void seedPrompt(String code, String name, String entityClass, String promptText, String placeholders) {
        AiPromptTemplate t = dataManager.create(AiPromptTemplate.class);
        t.setCode(code);
        t.setName(name);
        t.setEntityClass(entityClass);
        t.setPromptText(promptText);
        t.setAvailablePlaceholders(placeholders);
        t.setActive(true);
        dataManager.commit(t);
    }
}
