package com.company.hunttech.service;

/**
 * Сервис умной текстовой обработки и типографического AI-форматирования текстов
 * (резюме кандидатов, описаний вакансий, сопроводительных писем).
 *
 * <p>Предоставляет методы структурирования «сырого» текста в чистый, читаемый HTML-формат
 * или нормализованный Plain Text (TXT) с сохранением полного исходного содержания и фактов
 * (без сокращений и смысловых искажений).</p>
 *
 * <p>Поддерживает единый AI Control Plane HRM HuntTech: сначала проверяется наличие персонального
 * ключа пользователя (USER_OVERRIDE_ALLOWED), при отсутствии или сбое используется корпоративный
 * ключ администратора (FALLBACK_TO_ADMIN). Все prompt-шаблоны и конфигурации хранятся в БД
 * (HUNTTECH_AI_FUNCTION_CONFIGURATION).</p>
 */
public interface TextProcessingService {

    String NAME = "hunttech_TextProcessingService";

    /**
     * Стабильный код AI-функции форматирования текста в читаемый структурированный HTML.
     */
    String FUNCTION_TEXT_SMART_FORMAT_HTML = "TEXT_SMART_FORMAT_HTML";

    /**
     * Стабильный код AI-функции форматирования текста в аккуратный txt (plain text).
     */
    String FUNCTION_TEXT_SMART_FORMAT_PLAIN = "TEXT_SMART_FORMAT_PLAIN";

    /**
     * Параметр prompt-шаблона: анализируемый/форматируемый текст.
     */
    String PARAM_SOURCE_TEXT = "sourceText";

    /**
     * Выполняет умное форматирование текста в читаемый, красиво структурированный HTML-вид
     * с сохранением полного содержания.
     *
     * @param rawText исходный «сырой» текст (plain text или неформатированный HTML)
     * @return стилизованный, структурированный HTML-текст
     */
    String formatHtml(String rawText);

    /**
     * Выполняет умное форматирование текста в читаемый HTML с возвратом метаданных AI-вызова
     * (модель, провайдер, собственник API USER/ADMIN) для пользовательской нотификации.
     *
     * @param rawText исходный текст
     * @return результат с отформатированным HTML и метаданными AI
     */
    TextProcessingResult formatHtmlWithResult(String rawText);

    /**
     * Выполняет умное структурирование текста в чистый и аккуратный Plain Text (TXT)
     * с логическими отступами, читаемыми разделителями секций и маркированными списками.
     *
     * @param rawText исходный текст
     * @return отформатированный чистый текст
     */
    String formatPlainText(String rawText);

    /**
     * Выполняет умное структурирование текста в чистый Plain Text с возвратом метаданных AI-вызова.
     *
     * @param rawText исходный текст
     * @return результат с отформатированным TXT и метаданными AI
     */
    TextProcessingResult formatPlainTextWithResult(String rawText);
}
