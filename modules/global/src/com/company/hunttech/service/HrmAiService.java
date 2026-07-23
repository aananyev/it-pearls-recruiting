package com.company.hunttech.service;

import com.company.hunttech.entity.UserAiConfiguration;

public interface HrmAiService {
    String NAME = "hunttech_HrmAiService";

    /**
     * Строит стандартизированное описание вакансии через активную AI-конфигурацию
     * текущего пользователя для выбранного провайдера.
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
}
