package com.company.hunttech.service;

/**
 * Domain facade AI-операций проекта.
 *
 * Бизнес-экран передаёт только контекст проекта. Prompt, provider, model,
 * credential и fallback выбираются централизованно через AI Control Plane.
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
     * Доступные переменные административного prompt template:
     * projectName, sourceFileName, sourceText.
     */
    String processUploadedDescription(String projectName, String sourceFileName, String sourceText);

    /**
     * Генерирует краткое описание сути проекта (одно предложение) на
     * основании полного описания.
     *
     * Доступные переменные административного prompt template:
     * projectName, sourceText.
     */
    String generateShortDescription(String projectName, String descriptionText);
}
