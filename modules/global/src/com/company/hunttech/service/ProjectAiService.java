package com.company.hunttech.service;

/**
 * Domain facade AI-операций проекта.
 *
 * <p>Бизнес-экран передаёт только контекст проекта. Prompt, provider, model,
 * credential и fallback выбираются централизованно через AI Control Plane.</p>
 *
 * <p><b>Контракт пользовательской нотификации</b> (полный текст —
 * {@code docs/architecture/HRM_HuntTech_AI_User_Notification_Contract.md}): методы
 * возвращают {@link AiExecutionResult} — сгенерированный текст ({@code getText()}) и
 * метаданные выполнения (модель, провайдер, собственник API). Экран обязан показать
 * исчезающую TRAY-нотификацию (CUBA Notifications, web-утилита AiOperationNotifier)
 * с указанием, какая модель что сделала и чей API использован.</p>
 */
public interface ProjectAiService {
    String NAME = "hunttech_ProjectAiService";

    /**
     * Стабильный код функции обработки описания проекта. Администратор меняет
     * prompt/model/policy в AiFunctionConfiguration без изменений ProjectEdit.
     */
    String FUNCTION_PROJECT_DESCRIPTION_GENERATE = "PROJECT_DESCRIPTION_GENERATE";

    /**
     * Стабильный код функции генерации краткого описания сути проекта
     * (кнопка «Кратко» во вкладке «Описание проекта» ProjectEdit, capability
     * TEXT_GENERATION). Администратор меняет prompt/model/policy в
     * AiFunctionConfiguration без изменений ProjectEdit.
     */
    String FUNCTION_PROJECT_SHORT_DESCRIPTION_GENERATE = "PROJECT_SHORT_DESCRIPTION_GENERATE";

    /**
     * Обрабатывает текст, извлечённый из загруженного файла описания проекта.
     *
     * <p>Доступные переменные административного prompt template:
     * projectName, sourceFileName, sourceText.</p>
     *
     * @return результат AI-выполнения: обработанный текст ({@code getText()}) + метаданные
     *         (модель, провайдер, собственник API)
     */
    AiExecutionResult processUploadedDescription(String projectName, String sourceFileName, String sourceText);

    /**
     * Генерирует краткое описание сути проекта (одно предложение) на
     * основании полного описания.
     *
     * <p>Доступные переменные административного prompt template:
     * projectName, sourceText.</p>
     *
     * @return результат AI-выполнения: сгенерированный текст ({@code getText()}) + метаданные
     *         (модель, провайдер, собственник API)
     */
    AiExecutionResult generateShortDescription(String projectName, String descriptionText);
}
