package com.company.hunttech.service;

import com.company.hunttech.entity.SkillTree;

import java.util.List;

/**
 * Сервис анализа навыков в тексте резюме кандидата или описания вакансии.
 *
 * <p>На вход — произвольный текст (резюме или описание вакансии), на выходе — коллекция
 * навыков из справочника {@link SkillTree}, которые упоминаются в тексте. Извлечение
 * названий навыков выполняет нейросеть через {@link AiExecutionService} (стабильный
 * function code {@value #FUNCTION_SKILLS_EXTRACT}, capability TEXT_GENERATION); сервис
 * сопоставляет найденные названия со справочником skilltree и возвращает сущности.
 * Навыки, найденные нейросетью, но отсутствующие в справочнике, пишутся в лог (WARN)
 * — администратор анализирует их и добавляет в {@code HUNTTECH_SKILL_TREE}.</p>
 *
 * <p>Уровни анализа:</p>
 * <ul>
 *     <li>{@value #LEVEL_ALL} — все навыки, упомянутые в тексте;</li>
 *     <li>{@value #LEVEL_MAIN} — основные/обязательные навыки (в вакансии — обязательные
 *     требования, в резюме — ключевые навыки);</li>
 *     <li>{@value #LEVEL_SECONDARY} — второстепенные/желательные навыки (в вакансии —
 *     «желательно», в резюме — дополнительные навыки);</li>
 *     <li>{@value #LEVEL_TERTIARY} — третьестепенные навыки, если такие есть.</li>
 * </ul>
 *
 * <p>Промпт, модель и политики функции настраивает администратор в «Управление AI»
 * (таблица {@code HUNTTECH_AI_FUNCTION_CONFIGURATION}) без выпуска кода. При недоступности
 * AI (функция не активна, нет credentials, ошибка провайдера) сервис бесшовно переходит
 * на классический словарный поиск навыков по тексту — анализ не прерывается.</p>
 */
public interface SkillAnalysisService {

    String NAME = "hunttech_SkillAnalysisService";

    /**
     * Стабильный код AI-функции извлечения навыков из текста (capability TEXT_GENERATION).
     */
    String FUNCTION_SKILLS_EXTRACT = "SKILLS_EXTRACT";

    /**
     * Параметр prompt-шаблона: анализируемый текст.
     */
    String PARAM_SOURCE_TEXT = "sourceText";

    /**
     * Параметр prompt-шаблона: уровень анализа (ALL/MAIN/SECONDARY/TERTIARY).
     */
    String PARAM_SKILL_LEVEL = "skillLevel";

    String LEVEL_ALL = "ALL";
    String LEVEL_MAIN = "MAIN";
    String LEVEL_SECONDARY = "SECONDARY";
    String LEVEL_TERTIARY = "TERTIARY";

    /**
     * Анализирует текст и возвращает ВСЕ навыки из справочника {@link SkillTree},
     * которые в нём упоминаются.
     *
     * @param sourceText текст резюме кандидата или описания вакансии
     * @return коллекция навыков справочника (без дубликатов, в порядке обнаружения);
     *         навыки, отсутствующие в справочнике, записываются в лог (WARN)
     * @throws com.haulmont.cuba.core.global.DevelopmentException если текст пуст
     *         или превышает допустимый размер
     */
    List<SkillTree> analyzeAll(String sourceText);

    /**
     * Анализирует текст и возвращает основные/обязательные навыки.
     * Для описания вакансии — обязательные требования, для резюме — ключевые навыки.
     */
    List<SkillTree> analyzeMain(String sourceText);

    /**
     * Анализирует текст и возвращает второстепенные/желательные навыки.
     * Для описания вакансии — навыки «желательно», для резюме — дополнительные навыки.
     */
    List<SkillTree> analyzeSecondary(String sourceText);

    /**
     * Анализирует текст и возвращает третьестепенные навыки, если такие упоминаются
     * (редко встречающиеся, не ключевые). Если третьестепенных навыков нет — пустой список.
     */
    List<SkillTree> analyzeTertiary(String sourceText);
}
