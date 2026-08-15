package com.company.hunttech.service;

import com.company.hunttech.entity.SkillTree;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Чистые unit-тесты словарного матчинга {@link SkillNameMatcher} (без CUBA-контейнера).
 */
public class SkillNameMatcherTest {

    private SkillTree skill(String name) {
        SkillTree skill = new SkillTree();
        skill.setId(UUID.randomUUID());
        skill.setSkillName(name);
        return skill;
    }

    private List<SkillTree> dictionary(SkillTree... skills) {
        return Arrays.asList(skills);
    }

    @Test
    public void exactMatchIsCaseInsensitive() {
        SkillTree java = skill("Java");
        SkillNameMatcher.Result result =
                SkillNameMatcher.matchNames(dictionary(java), Arrays.asList("java", "JAVA", " Java "));
        assertEquals(1, result.getMatched().size());
        assertEquals("Java", result.getMatched().get(0).getSkillName());
        assertTrue(result.getUnknown().isEmpty());
    }

    @Test
    public void multiWordSkillMatchesContiguousTokens() {
        SkillTree javaEe = skill("Java EE");
        SkillNameMatcher.Result result =
                SkillNameMatcher.matchNames(dictionary(javaEe), Arrays.asList("Java EE", "знание java ee"));
        assertEquals(1, result.getMatched().size());
        assertEquals("Java EE", result.getMatched().get(0).getSkillName());
    }

    @Test
    public void javaDoesNotMatchJavaScript() {
        SkillTree java = skill("Java");
        SkillTree javaScript = skill("JavaScript");
        SkillNameMatcher.Result result = SkillNameMatcher.matchNames(
                dictionary(java, javaScript), Arrays.asList("JavaScript"));
        assertEquals(1, result.getMatched().size());
        assertEquals("JavaScript", result.getMatched().get(0).getSkillName());
    }

    @Test
    public void compoundNameMatchesSeveralSkills() {
        SkillTree java = skill("Java");
        SkillTree spring = skill("Spring");
        SkillNameMatcher.Result result = SkillNameMatcher.matchNames(
                dictionary(java, spring), Arrays.asList("Java Spring"));
        assertEquals(2, result.getMatched().size());
        assertTrue(result.getMatched().contains(java));
        assertTrue(result.getMatched().contains(spring));
    }

    @Test
    public void specialCharactersTokensArePreserved() {
        SkillTree cpp = skill("C++");
        SkillTree csharp = skill("C#");
        SkillNameMatcher.Result result = SkillNameMatcher.matchNames(
                dictionary(cpp, csharp), Arrays.asList("C++", "C#"));
        assertEquals(2, result.getMatched().size());
    }

    @Test
    public void russianSkillMatches() {
        SkillTree oneC = skill("1С");
        SkillTree result = SkillNameMatcher.matchNames(
                dictionary(oneC), Arrays.asList("1С")).getMatched().get(0);
        assertNotNull(result);
        assertEquals("1С", result.getSkillName());
    }

    @Test
    public void unknownNamesAreReported() {
        SkillTree java = skill("Java");
        SkillNameMatcher.Result result = SkillNameMatcher.matchNames(
                dictionary(java), Arrays.asList("Java", "Kotlin", "Rust"));
        assertEquals(1, result.getMatched().size());
        assertEquals(Arrays.asList("Kotlin", "Rust"), result.getUnknown());
    }

    @Test
    public void duplicatesAreDeduplicated() {
        SkillTree java = skill("Java");
        SkillNameMatcher.Result result = SkillNameMatcher.matchNames(
                dictionary(java), Arrays.asList("Java", "java", " Java "));
        assertEquals(1, result.getMatched().size());
    }

    @Test
    public void exactMatchHasPriorityOverTokenSplit() {
        SkillTree javaCore = skill("Java Core");
        SkillTree java = skill("Java");
        // Точное совпадение «Java Core» — только сам навык, без подтягивания «Java».
        SkillNameMatcher.Result result = SkillNameMatcher.matchNames(
                dictionary(java, javaCore), Arrays.asList("Java Core"));
        assertEquals(1, result.getMatched().size());
        assertEquals("Java Core", result.getMatched().get(0).getSkillName());
    }

    @Test
    public void matchTextFindsDictionarySkillsInFreeText() {
        SkillTree java = skill("Java");
        SkillTree spring = skill("Spring");
        SkillTree javaScript = skill("JavaScript");
        List<SkillTree> found = SkillNameMatcher.matchText(
                dictionary(java, spring, javaScript),
                "5 лет опыта Java и Spring, немного JavaScript.");
        assertEquals(3, found.size());
    }

    @Test
    public void matchTextIgnoresNullOrBlankText() {
        assertTrue(SkillNameMatcher.matchText(dictionary(skill("Java")), null).isEmpty());
        assertTrue(SkillNameMatcher.matchText(dictionary(skill("Java")), "   ").isEmpty());
    }

    @Test
    public void normalizeHandlesNullAndPunctuation() {
        assertEquals("", SkillNameMatcher.normalize(null));
        assertEquals("java ee", SkillNameMatcher.normalize("  Java   EE  "));
    }
}
