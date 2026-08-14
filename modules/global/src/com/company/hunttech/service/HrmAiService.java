package com.company.hunttech.service;

import com.company.hunttech.entity.UserAiConfiguration;

import java.util.UUID;

public interface HrmAiService {
    String NAME = "hunttech_HrmAiService";

    String FUNCTION_STANDARDIZE_VACANCY = "STANDARDIZE_VACANCY";

    /**
     * Строит стандартизированное описание вакансии через сохранённую
     * AI-конфигурацию текущего пользователя для явно выбранного провайдера.
     */
    String standardizeVacancyDescription(String rawText);

    /**
     * Генерирует vacancy-артефакт через AI-функцию с переданным стабильным кодом.
     */
    String generateVacancyArtifact(String standardizedDescription, String functionCode);

    /**
     * Legacy overload сохранён для бинарной/исходной совместимости старых потребителей.
     * providerCode больше не влияет на маршрутизацию: её определяет AI Control Plane.
     */
    @Deprecated
    String standardizeVacancyDescription(String rawText, String providerCode);

    /**
     * Legacy overload сохранён для совместимости. templateCode трактуется как
     * functionCode, а providerCode игнорируется централизованным resolver.
     */
    @Deprecated
    String generateVacancyArtifact(String standardizedDescription, String templateCode, String providerCode);

    /**
     * Выполняет реальный лёгкий запрос конкретным пользовательским credential.
     * Это диагностическая операция подключения, а не рабочая AI-функция приложения.
     */
    void testConnection(UserAiConfiguration configuration);

    /**
     * Отправляет произвольный промпт явно выбранному провайдеру.
     * Используется сценариями, где провайдер выбирается пользователем непосредственно.
     */
    String sendPrompt(String userPrompt, String providerCode);

    /**
     * Отправляет системный промпт через единственную текущую AI-конфигурацию
     * пользователя. Этим методом пользуются кнопки AI-анализа на экранах сущностей.
     */
    String sendPromptUsingCurrentConfiguration(String userPrompt);

    /**
     * Делает выбранную конфигурацию единственной текущей для её пользователя.
     *
     * @param configurationId идентификатор UserAiConfiguration
     */
    void setCurrentConfiguration(UUID configurationId);
}
