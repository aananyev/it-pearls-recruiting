package com.company.hunttech.service;

import com.company.hunttech.entity.UserAiConfiguration;

/**
 * AI-фасад HRM HuntTech для vacancy-сценариев и проверки пользовательских подключений.
 *
 * Рабочая генерация маршрутизируется только через AiExecutionService и стабильный
 * functionCode. Выбор provider/model/credential выполняется AI Control Plane.
 */
public interface HrmAiService {
    String NAME = "hunttech_HrmAiService";

    String FUNCTION_STANDARDIZE_VACANCY = "STANDARDIZE_VACANCY";

    /**
     * Стандартизирует исходное описание вакансии через функцию STANDARDIZE_VACANCY.
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
}
