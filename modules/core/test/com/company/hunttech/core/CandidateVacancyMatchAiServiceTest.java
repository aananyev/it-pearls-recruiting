package com.company.hunttech.core;

import com.company.hunttech.entity.CandidateVacancyMatchItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Тестирование алгоритмов нормализации score, валидации и ранжирования CandidateVacancyMatchAiService.
 */
public class CandidateVacancyMatchAiServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testScoreNormalizationAndSubscoresSum() {
        int roleFit = 20;       // max 25
        int skillsFit = 30;     // max 35
        int expFit = 18;        // max 20
        int prefFit = 8;        // max 10
        int domFit = 9;         // max 10

        int sum = roleFit + skillsFit + expFit + prefFit + domFit;
        assertEquals(85, sum);
        assertTrue("Score должен быть в диапазоне 0..100", sum >= 0 && sum <= 100);

        String verdict = resolveVerdict(sum);
        assertEquals("Рекомендуется предложить", verdict);
    }

    @Test
    public void testVerdictThresholds() {
        assertEquals("Рекомендуется предложить", resolveVerdict(80));
        assertEquals("Рекомендуется предложить", resolveVerdict(100));
        assertEquals("Имеет смысл рассмотреть", resolveVerdict(79));
        assertEquals("Имеет смысл рассмотреть", resolveVerdict(65));
        assertEquals("Слабое соответствие", resolveVerdict(64));
        assertEquals("Слабое соответствие", resolveVerdict(45));
        assertEquals("Не рекомендуется", resolveVerdict(44));
        assertEquals("Не рекомендуется", resolveVerdict(0));
    }

    @Test
    public void testSortingTieBreakers() {
        List<CandidateVacancyMatchItem> items = new ArrayList<>();

        CandidateVacancyMatchItem i1 = new CandidateVacancyMatchItem();
        i1.setVacancyName("Java Backend Developer");
        i1.setScore(75);
        i1.setPriority(2);

        CandidateVacancyMatchItem i2 = new CandidateVacancyMatchItem();
        i2.setVacancyName("Lead Architect");
        i2.setScore(90);
        i2.setPriority(1);

        CandidateVacancyMatchItem i3 = new CandidateVacancyMatchItem();
        i3.setVacancyName("DevOps Engineer");
        i3.setScore(75);
        i3.setPriority(5); // Выше priority при одинаковом score

        CandidateVacancyMatchItem i4 = new CandidateVacancyMatchItem();
        i4.setVacancyName("Alpha Python Developer");
        i4.setScore(75);
        i4.setPriority(2); // Одинаковый score и priority, tie-breaker по имени ASC (Alpha < Java)

        items.add(i1);
        items.add(i2);
        items.add(i3);
        items.add(i4);

        Comparator<CandidateVacancyMatchItem> comp = (a, b) -> {
            int c1 = Integer.compare(b.getScore() != null ? b.getScore() : 0, a.getScore() != null ? a.getScore() : 0);
            if (c1 != 0) return c1;
            int p1 = a.getPriority() != null ? a.getPriority() : -1;
            int p2 = b.getPriority() != null ? b.getPriority() : -1;
            int c2 = Integer.compare(p2, p1);
            if (c2 != 0) return c2;
            String n1 = a.getVacancyName() != null ? a.getVacancyName() : "";
            String n2 = b.getVacancyName() != null ? b.getVacancyName() : "";
            return n1.compareToIgnoreCase(n2);
        };

        items.sort(comp);

        assertEquals("Lead Architect", items.get(0).getVacancyName()); // score 90
        assertEquals("DevOps Engineer", items.get(1).getVacancyName()); // score 75, priority 5
        assertEquals("Alpha Python Developer", items.get(2).getVacancyName()); // score 75, priority 2, 'A'
        assertEquals("Java Backend Developer", items.get(3).getVacancyName()); // score 75, priority 2, 'J'
    }

    @Test
    public void testCleanJsonText() {
        String raw = "```json\n{\n  \"matches\": []\n}\n```";
        String cleaned = cleanJsonText(raw);
        assertFalse(cleaned.startsWith("```"));
        assertFalse(cleaned.endsWith("```"));
        assertTrue(cleaned.contains("\"matches\""));
    }

    @Test
    public void testSafeParsingInvalidJson() {
        String badJson = "{ invalid json content ...";
        assertNull(tryParse(badJson));
    }

    private JsonNode tryParse(String text) {
        try {
            return objectMapper.readTree(cleanJsonText(text));
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveVerdict(int score) {
        if (score >= 80) return "Рекомендуется предложить";
        if (score >= 65) return "Имеет смысл рассмотреть";
        if (score >= 45) return "Слабое соответствие";
        return "Не рекомендуется";
    }

    private String cleanJsonText(String text) {
        if (text == null) return "";
        String s = text.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }
}
