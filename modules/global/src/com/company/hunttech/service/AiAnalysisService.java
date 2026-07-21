package com.company.hunttech.service;

import com.haulmont.cuba.core.entity.Entity;

/**
 * Сервис AI-анализа сущностей.
 * Получает сущность и код промпта, заполняет шаблон данными,
 * вызывает AI-провайдера и возвращает текстовый результат.
 */
public interface AiAnalysisService {

    String NAME = "hunttech_AiAnalysisService";

    /**
     * @param entity     экземпляр сущности для анализа
     * @param promptCode код промпта из AiPromptTemplate.code
     * @return текстовый ответ AI
     */
    String analyze(Entity entity, String promptCode);
}
