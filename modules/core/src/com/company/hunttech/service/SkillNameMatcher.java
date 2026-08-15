package com.company.hunttech.service;

import com.company.hunttech.entity.SkillTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Словарный матчинг названий навыков справочника {@link SkillTree} против текста
 * или списка названий, возвращённых нейросетью.
 *
 * <p>Чистая функция без зависимостей от CUBA-контейнера и AI: получает на вход
 * справочник (список {@link SkillTree}) и ищет его названия в целевом тексте
 * (классический fallback) либо сопоставляет пришедшие извне названия со справочником
 * (режим «нейросеть уже извлекла названия»). Логику разделения на «найденные» и
 * «неизвестные» возвращает в {@link Result} — вызывающий код сам решает, что делать
 * с неизвестными (сервис пишет их в лог для администратора).</p>
 *
 * <p>Сопоставление по токенам (атомарным словам):</p>
 * <ul>
 *     <li>нормализация: нижний регистр, обрезка, схлопывание пробелов;</li>
 *     <li>токенизация по пробелам с удалением обрамляющей пунктуации — внутренние
 *         спецсимволы сохраняются: {@code C++} → {@code c++}, {@code C#} → {@code c#},
 *         {@code ASP.NET} → {@code asp.net};</li>
 *     <li>совпадение — точная последовательность токенов: {@code Java EE} совпадает
 *         с {@code java ee}, но {@code Java} НЕ совпадает с {@code JavaScript}
 *         (разные токены);</li>
 *     <li>название из нейросети может дать несколько навыков справочника:
 *         {@code Java Spring} → {@code Java} + {@code Spring} (родственные/вложенные
 *         навыки); точное совпадение всего названия имеет приоритет;</li>
 *     <li>дубликаты исключаются по идентификатору сущности.</li>
 * </ul>
 */
public final class SkillNameMatcher {

    private SkillNameMatcher() {
    }

    /**
     * Результат сопоставления: найденные навыки справочника и названия, которым
     * не нашлось соответствия в справочнике.
     */
    public static final class Result {
        private final List<SkillTree> matched;
        private final List<String> unknown;

        private Result(List<SkillTree> matched, List<String> unknown) {
            this.matched = Collections.unmodifiableList(matched);
            this.unknown = Collections.unmodifiableList(unknown);
        }

        public List<SkillTree> getMatched() {
            return matched;
        }

        public List<String> getUnknown() {
            return unknown;
        }
    }

    /**
     * Сопоставляет названия навыков (например, из ответа нейросети) со справочником.
     *
     * <p>Порядок: точное совпадение всего названия (по нормализованной форме) →
     * иначе поиск всех навыков справочника, чьи токены входят в название как
     * непрерывная последовательность. Найденные сущности возвращаются без
     * дубликатов, в порядке следования названий.</p>
     *
     * @param dictionary справочник навыков (уже без {@code notParsing})
     * @param names      названия навыков, найденные нейросетью
     * @return найденные сущности + названия без соответствия
     */
    public static Result matchNames(List<SkillTree> dictionary, List<String> names) {
        Map<String, SkillTree> dictionaryByNormalized = buildNormalizedMap(dictionary);
        List<SkillTree> matched = new ArrayList<>();
        Set<String> matchedIds = new LinkedHashSet<>();
        List<String> unknown = new ArrayList<>();

        for (String rawName : names) {
            if (rawName == null) {
                continue;
            }
            String normalized = normalize(rawName);
            if (normalized.isEmpty()) {
                continue;
            }
            SkillTree exact = dictionaryByNormalized.get(normalized);
            if (exact != null) {
                addUnique(matched, matchedIds, exact);
                continue;
            }
            List<String> nameTokens = tokenize(normalized);
            if (nameTokens.isEmpty()) {
                continue;
            }
            boolean anyMatched = false;
            for (SkillTree skill : dictionary) {
                if (containsTokenSequence(nameTokens, tokenize(normalize(skill.getSkillName())))) {
                    addUnique(matched, matchedIds, skill);
                    anyMatched = true;
                }
            }
            if (!anyMatched) {
                unknown.add(rawName.trim());
            }
        }
        return new Result(matched, unknown);
    }

    /**
     * Ищет навыки справочника в тексте (классический fallback без нейросети):
     * каждый навык справочника считается найденным, если его токены встречаются
     * в тексте как непрерывная последовательность.
     *
     * @param dictionary справочник навыков
     * @param text       анализируемый текст
     * @return найденные навыки без дубликатов
     */
    public static List<SkillTree> matchText(List<SkillTree> dictionary, String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> textTokens = tokenize(normalize(text));
        List<SkillTree> matched = new ArrayList<>();
        Set<String> matchedIds = new LinkedHashSet<>();

        for (SkillTree skill : dictionary) {
            String normalized = normalize(skill.getSkillName());
            if (normalized.isEmpty()) {
                continue;
            }
            List<String> skillTokens = tokenize(normalized);
            if (skillTokens.isEmpty() || !containsTokenSequence(textTokens, skillTokens)) {
                continue;
            }
            addUnique(matched, matchedIds, skill);
        }
        return matched;
    }

    /**
     * Нормализует название: нижний регистр, обрезка, схлопывание пробелов.
     */
    static String normalize(String value) {
        return value == null ? "" : value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Разбивает нормализованную строку на атомарные токены по пробелам,
     * удаляя обрамляющую пунктуацию (внутренние спецсимволы сохраняются).
     */
    static List<String> tokenize(String normalized) {
        if (normalized == null || normalized.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>();
        for (String part : normalized.split(" ")) {
            String token = stripOuterPunctuation(part);
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static String stripOuterPunctuation(String token) {
        int start = 0;
        int end = token.length();
        while (start < end && !Character.isLetterOrDigit(token.charAt(start))) {
            start++;
        }
        while (end > start && !Character.isLetterOrDigit(token.charAt(end - 1))) {
            end--;
        }
        return token.substring(start, end);
    }

    private static Map<String, SkillTree> buildNormalizedMap(List<SkillTree> dictionary) {
        Map<String, SkillTree> map = new LinkedHashMap<>();
        for (SkillTree skill : dictionary) {
            String normalized = normalize(skill.getSkillName());
            if (!normalized.isEmpty()) {
                map.putIfAbsent(normalized, skill);
            }
        }
        return map;
    }

    private static void addUnique(List<SkillTree> matched, Set<String> matchedIds, SkillTree skill) {
        if (skill.getId() != null && !matchedIds.add(skill.getId().toString())) {
            return;
        }
        matched.add(skill);
    }

    /**
     * Проверяет, что последовательность {@code needle} встречается в {@code haystack}
     * как непрерывная подпоследовательность (начиная с любого индекса).
     */
    private static boolean containsTokenSequence(List<String> haystack, List<String> needle) {
        if (needle.isEmpty()) {
            return true;
        }
        if (haystack.size() < needle.size()) {
            return false;
        }
        outer:
        for (int i = 0; i <= haystack.size() - needle.size(); i++) {
            for (int j = 0; j < needle.size(); j++) {
                if (!haystack.get(i + j).equals(needle.get(j))) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
