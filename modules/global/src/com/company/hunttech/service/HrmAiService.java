package com.company.hunttech.service;

import com.company.hunttech.entity.UserAiConfiguration;

import java.util.UUID;

public interface HrmAiService {
    String NAME = "hunttech_HrmAiService";

    /**
     * Строит стандартизированное описание вакансии через сохранённую
     * AI-конфигурацию текущего пользователя для явно выбранного провайдера.
     */
    String standardizeVacancyDescription(String rawText, String providerCode);

    /**
     * Генерирует артефакт вакансии, применяя prompt-шаблон к уже
     * стандартизированному описанию вакансии.
     */
    String generateVacancyArtifact(String standardizedDescription, String templateCode, String providerCode);

    /**
     * Выполняет реальный лёгкий запрос к провайдеру из переданной пользовательской
     * конфигурации. Метод используется экранами настроек, чтобы проверить код
     * провайдера, API-ключ и модель до запуска рабочих AI-сценариев рекрутинга.
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
