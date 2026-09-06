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
    String FUNCTION_VACANCY_CHECKLIST = "VACANCY_CHECKLIST";
    String FUNCTION_VACANCY_SEARCH_MAP = "VACANCY_SEARCH_MAP";
    String FUNCTION_VACANCY_INTERVIEW_PLAN = "VACANCY_INTERVIEW_PLAN";

    /**
     * Код диагностической AI-функции «тест подключения» (проверка provider/key/model
     * до назначения credential на рабочую функцию). Используется в метаданных
     * результата {@link AiExecutionResult} для нотификации (контракт пользовательской
     * нотификации).
     */
    String FUNCTION_TEST_CONNECTION = "TEST_CONNECTION";

    /**
     * Стандартизирует исходное описание вакансии через функцию STANDARDIZE_VACANCY.
     */
    String standardizeVacancyDescription(String rawText);

    /**
     * Генерирует Must-have чек-лист требований к кандидату через функцию VACANCY_CHECKLIST.
     */
    String generateChecklist(String vacancyText);

    /**
     * Генерирует Карту поиска и сорсинга рекрутера через функцию VACANCY_SEARCH_MAP.
     */
    String generateSearchMap(String vacancyText);

    /**
     * Генерирует План продающего собеседования через функцию VACANCY_INTERVIEW_PLAN.
     */
    String generateInterviewPlan(String vacancyText);

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
     *
     * <p>Контракт пользовательской нотификации: возвращает {@link AiExecutionResult}
     * с метаданными (модель, провайдер, собственник API = личный ключ пользователя),
     * чтобы экраны «Управление AI» показывали исчезающую нотификацию «какая модель
     * что делала + чей API» наравне с рабочими операциями.</p>
     */
    AiExecutionResult testConnection(UserAiConfiguration configuration);
}
