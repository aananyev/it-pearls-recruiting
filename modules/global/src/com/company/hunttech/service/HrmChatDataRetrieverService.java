package com.company.hunttech.service;

import com.company.hunttech.dto.HrmDataContextSnapshot;

/**
 * Сервис извлечения среза данных HRM в строгом режиме чтения (READ-ONLY)
 * для обогащения контекста LLM-чата.
 */
public interface HrmChatDataRetrieverService {
    String NAME = "hunttech_HrmChatDataRetrieverService";

    /**
     * Анализирует пользовательский запрос и, если запрос требует обращения к данным HRM
     * (вакансии, кандидаты, резюме, взаимодействия, справочники), извлекает релевантный срез.
     *
     * @param userMessage текст запроса пользователя
     * @return снимок данных в компактном формате Markdown, либо пустой снимок
     */
    HrmDataContextSnapshot retrieveContextForMessage(String userMessage);

    /**
     * Извлекает срез данных с явным указанием максимального количества сущностей каждого типа.
     *
     * @param userMessage        текст запроса пользователя
     * @param maxEntitiesPerType лимит сущностей каждого типа (вакансии, кандидаты, взаимодействия)
     * @return снимок данных
     */
    HrmDataContextSnapshot retrieveContextForMessage(String userMessage, int maxEntitiesPerType);
}
