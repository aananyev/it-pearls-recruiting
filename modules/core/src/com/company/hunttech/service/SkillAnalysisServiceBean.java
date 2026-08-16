package com.company.hunttech.service;

import com.company.hunttech.entity.SkillTree;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.LoadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Реализация {@link SkillAnalysisService}: анализ текста резюме/вакансии нейросетью
 * с сопоставлением найденных названий со справочником {@link SkillTree}.
 *
 * <p>Алгоритм:</p>
 * <ol>
 *     <li>загрузка справочника навыков (без {@code notParsing = true}) — view
 *         {@code skillTree-parser-view};</li>
 *     <li>вызов AI-функции {@value SkillAnalysisService#FUNCTION_SKILLS_EXTRACT}
 *         (capability TEXT_GENERATION) с контекстом {@code sourceText} +
 *         {@code skillLevel} — нейросеть возвращает JSON-массив названий навыков;</li>
 *     <li>разбор ответа (устойчивый к markdown-ограждениям и построчному формату);</li>
 *     <li>сопоставление названий со справочником {@link SkillNameMatcher}: найденные
 *         навыки возвращаются, не найденные пишутся в лог (WARN) — администратор
 *         анализирует их и добавляет в {@code HUNTTECH_SKILL_TREE};</li>
 *     <li>при любой недоступности AI (функция не активна, нет credentials, ошибка
 *         провайдера) — бесшовный классический fallback: {@link SkillNameMatcher#matchText}
 *         ищет навыки справочника прямо в тексте; анализ не прерывается, метаданные
 *         AI-выполнения в результате равны {@code null}.</li>
 * </ol>
 *
 * <p>Промпт, модель и политики функции настраиваются администратором в «Управление AI»
 * (таблица {@code HUNTTECH_AI_FUNCTION_CONFIGURATION}) — код сервиса их не содержит.</p>
 *
 * <p>Результат каждого метода — {@link SkillAnalysisResult}: навыки + метаданные
 * AI-выполнения ({@link AiExecutionResult}: модель, провайдер, собственник API) для
 * контракта пользовательской нотификации (см. {@code docs/architecture/HRM_HuntTech_AI_User_Notification_Contract.md}).</p>
 */
@Service(SkillAnalysisService.NAME)
public class SkillAnalysisServiceBean implements SkillAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(SkillAnalysisServiceBean.class);

    /**
     * Максимальная длина анализируемого текста (как в ProjectAiService).
     */
    private static final int MAX_SOURCE_TEXT_LENGTH = 120_000;

    /**
     * Запрос справочника навыков: активные навыки, не помеченные «не парсить».
     * Мягко удалённые записи DataManager исключает автоматически.
     */
    private static final String DICTIONARY_QUERY =
            "select e from hunttech_SkillTree e where (e.notParsing is null or e.notParsing = false)";

    /**
     * View справочника для парсинга: skillName + notParsing + родительский навык.
     */
    private static final String DICTIONARY_VIEW = "skillTree-parser-view";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private AiExecutionService aiExecutionService;

    @Inject
    private DataManager dataManager;

    @Override
    public SkillAnalysisResult analyzeAll(String sourceText) {
        return analyze(sourceText, LEVEL_ALL);
    }

    @Override
    public SkillAnalysisResult analyzeMain(String sourceText) {
        return analyze(sourceText, LEVEL_MAIN);
    }

    @Override
    public SkillAnalysisResult analyzeSecondary(String sourceText) {
        return analyze(sourceText, LEVEL_SECONDARY);
    }

    @Override
    public SkillAnalysisResult analyzeTertiary(String sourceText) {
        return analyze(sourceText, LEVEL_TERTIARY);
    }

    private SkillAnalysisResult analyze(String sourceText, String skillLevel) {
        String normalizedText = validateAndNormalize(sourceText);
        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put(PARAM_SOURCE_TEXT, normalizedText);
            context.put(PARAM_SKILL_LEVEL, skillLevel);
            AiExecutionResult execution = aiExecutionService.executeText(FUNCTION_SKILLS_EXTRACT, context);
            List<SkillTree> matched = matchAgainstDictionary(parseSkillNames(execution.getText()));
            return SkillAnalysisResult.of(matched, execution);
        } catch (RuntimeException e) {
            // AI недоступен (функция не активна, нет credentials, ошибка провайдера) —
            // бесшовный классический fallback: прямой словарный поиск в тексте.
            // Метаданные AI-выполнения не заполняются — экран не показывает
            // нотификацию «обработано ИИ» (контракт пользовательской нотификации).
            log.warn("AI-анализ навыков (уровень {}) недоступен, используется классический "
                    + "словарный поиск. Причина: {}", skillLevel, e.toString());
            return SkillAnalysisResult.of(SkillNameMatcher.matchText(loadDictionary(), normalizedText), null);
        }
    }

    /**
     * Сопоставляет названия из ответа нейросети со справочником; неизвестные
     * названия пишет в лог (WARN) для администратора.
     */
    private List<SkillTree> matchAgainstDictionary(List<String> names) {
        SkillNameMatcher.Result result =
                SkillNameMatcher.matchNames(loadDictionary(), names);
        if (!result.getUnknown().isEmpty()) {
            log.warn("AI-анализ нашёл навыки, отсутствующие в справочнике skilltree — "
                    + "администратору добавить их в HUNTTECH_SKILL_TREE: {}",
                    result.getUnknown());
        }
        return result.getMatched();
    }

    private List<SkillTree> loadDictionary() {
        return dataManager.loadList(LoadContext.create(SkillTree.class)
                .setQuery(LoadContext.createQuery(DICTIONARY_QUERY))
                .setView(DICTIONARY_VIEW));
    }

    private String validateAndNormalize(String sourceText) {
        if (sourceText == null || sourceText.trim().isEmpty()) {
            throw new DevelopmentException("Текст для анализа навыков пуст.");
        }
        String normalized = sourceText.trim();
        if (normalized.length() > MAX_SOURCE_TEXT_LENGTH) {
            throw new DevelopmentException(
                    "Текст для анализа навыков слишком большой для AI-обработки "
                            + "(максимум " + MAX_SOURCE_TEXT_LENGTH + " символов).");
        }
        return normalized;
    }

    /**
     * Разбирает ответ нейросети в список названий навыков.
     *
     * <p>Устойчивость: принимает чистый JSON-массив, JSON-массив в markdown-ограждении
     * (```json … ```), а также построчный/запятый список без JSON. Пустой или
     * неразборчивый ответ даёт пустой список (пустой результат анализа).</p>
     */
    private List<String> parseSkillNames(String response) {
        if (response == null || response.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String candidate = response.trim()
                .replaceAll("^```(?:json)?\\s*", "")
                .replaceAll("\\s*```$", "");

        int start = candidate.indexOf('[');
        int end = candidate.lastIndexOf(']');
        if (start >= 0 && end > start) {
            String jsonBlock = candidate.substring(start, end + 1);
            try {
                JsonNode node = objectMapper.readTree(jsonBlock);
                if (node != null && node.isArray()) {
                    List<String> names = new ArrayList<>();
                    for (JsonNode item : node) {
                        if (item != null && item.isTextual() && !item.asText().trim().isEmpty()) {
                            names.add(item.asText().trim());
                        }
                    }
                    if (!names.isEmpty()) {
                        return names;
                    }
                }
            } catch (Exception e) {
                log.debug("JSON-разбор ответа AI не удался, используется построчное разбиение: {}",
                        e.toString());
            }
            return splitNames(candidate.substring(start + 1, end));
        }
        return splitNames(candidate);
    }

    /**
     * Разбивает текст ответа на названия по переводам строк и запятым,
     * снимает обрамляющие кавычки.
     */
    private List<String> splitNames(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        String[] parts = content.replace('\n', ',').split(",");
        for (String part : parts) {
            String name = part.trim();
            if (name.isEmpty()) {
                continue;
            }
            while (name.length() > 1
                    && (name.startsWith("\"") || name.startsWith("'") || name.startsWith("«"))
                    && (name.endsWith("\"") || name.endsWith("'") || name.endsWith("»"))) {
                name = name.substring(1, name.length() - 1).trim();
            }
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }
}
