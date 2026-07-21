package com.company.hunttech.web.screens.aiprompttemplate;

import com.company.hunttech.entity.AiPromptTemplate;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.util.List;

@UiController("hunttech_AiPromptTemplate.browse")
@UiDescriptor("ai-prompt-template-browse.xml")
@LoadDataBeforeShow
public class AiPromptTemplateBrowse extends StandardLookup<AiPromptTemplate> {

    @Inject
    private DataManager dataManager;

    @Subscribe
    public void onInit(InitEvent event) {
        // Если таблица пуста — создаём три базовых промпта
        List<AiPromptTemplate> existing = dataManager.load(AiPromptTemplate.class)
                .query("select e from hunttech_AiPromptTemplate e")
                .maxResults(1)
                .list();
        if (existing.isEmpty()) {
            seedPrompt("RESUME_ANALYSIS", "Анализ резюме",
                    "com.company.hunttech.entity.CandidateCV",
                    "Ты — опытный рекрутер HRM HuntTech. Проанализируй резюме кандидата.\n\n"
                    + "Имя: {{candidateName}}\nВакансия: {{positionName}}\n\n"
                    + "=== РЕЗЮМЕ ===\n{{resumeText}}\n\n"
                    + "=== ВАКАНСИЯ ===\n{{vacancyDescription}}\n\n"
                    + "Оцени соответствие по шкале 1-10. Выдели сильные и слабые стороны. Дай рекомендации.",
                    "{\"resumeText\":\"текст резюме\",\"candidateName\":\"ФИО\",\"vacancyDescription\":\"описание вакансии\",\"positionName\":\"позиция\"}");

            seedPrompt("VACANCY_ANALYSIS", "Расшифровка вакансии",
                    "com.company.hunttech.entity.OpenPosition",
                    "Ты — AI-ассистент рекрутера HRM HuntTech. Объясни вакансию.\n\n"
                    + "Компания: {{companyName}}\nПроект: {{projectName}}\n"
                    + "Описание: {{vacancyDescription}}\nТребования: {{vacancyRequirements}}\n\n"
                    + "Опиши суть вакансии, ключевые навыки, портрет кандидата, рекомендации по поиску.",
                    "{\"vacancyDescription\":\"описание\",\"vacancyRequirements\":\"требования\",\"companyName\":\"компания\",\"projectName\":\"проект\"}");

            seedPrompt("INTERACTION_ANALYSIS", "Анализ воронки кандидата",
                    "com.company.hunttech.entity.IteractionList",
                    "Ты — AI-ассистент рекрутера HRM HuntTech. Проанализируй воронку.\n\n"
                    + "Тип: {{interactionType}} | Дата: {{dateIteraction}} | Рекрутер: {{recrutierName}}\n"
                    + "Комментарий: {{comment}}\nКандидат: {{candidateName}}\n\n"
                    + "=== ИСТОРИЯ ===\n{{candidateHistory}}\n\n"
                    + "Определи стадию воронки, найди признаки зависания, дай прогноз и рекомендацию.",
                    "{\"interactionType\":\"тип\",\"dateIteraction\":\"дата\",\"recrutierName\":\"рекрутер\",\"comment\":\"комментарий\",\"candidateName\":\"ФИО\",\"candidateHistory\":\"история 20 записей\"}");
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
