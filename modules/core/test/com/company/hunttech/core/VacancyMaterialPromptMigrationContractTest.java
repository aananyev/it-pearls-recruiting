package com.company.hunttech.core;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VacancyMaterialPromptMigrationContractTest {

    @Test
    public void checklistPromptIsExactAndProductionSafe() throws Exception {
        assertMigration("260921-3-vacancy-checklist-prompt", "VACANCY_CHECKLIST",
                "ff2b07e9699ab5c838e91216e4157954c9dbc08f0380308504b306a0dac942c5");
    }

    @Test
    public void searchMapPromptIsExactAndProductionSafe() throws Exception {
        assertMigration("260921-4-vacancy-search-map-prompt", "VACANCY_SEARCH_MAP",
                "f6679ad6d9f4022a390908712379de4c6f2246cfa695c6c28a4399bde40f373b");
    }

    @Test
    public void interviewPlanPromptIsExactAndProductionSafe() throws Exception {
        assertMigration("260921-5-vacancy-interview-plan-prompt", "VACANCY_INTERVIEW_PLAN",
                "da661eaa0a1f49d3917b871f37191019c07f6c3de5ce7ef43518b8dbc42661d7");
    }

    @Test
    public void vacancyMaterialMigrationsAreIncludedInOrder() throws Exception {
        String master = readUtf8(Path.of("db/changelog/db.changelog-master.xml"));
        int checklist = master.indexOf("260921-3-vacancy-checklist-prompt.xml");
        int searchMap = master.indexOf("260921-4-vacancy-search-map-prompt.xml");
        int interviewPlan = master.indexOf("260921-5-vacancy-interview-plan-prompt.xml");

        assertTrue(checklist >= 0);
        assertTrue(checklist < searchMap);
        assertTrue(searchMap < interviewPlan);
    }

    private void assertMigration(String migration, String functionCode, String expectedPromptSha256)
            throws Exception {
        Path sqlPath = Path.of("db/update/postgres/26", migration + ".sql");
        if (!Files.exists(sqlPath)) {
            sqlPath = Path.of("modules/core/db/update/postgres/26", migration + ".sql");
        }
        String sql = readUtf8(sqlPath);
        String xml = readUtf8(Path.of("db/changelog", migration + ".xml"));
        String master = readUtf8(Path.of("db/changelog/db.changelog-master.xml"));

        assertTrue(sql.contains("CODE = '" + functionCode + "'"));
        assertTrue(sql.contains("'TEXT_GENERATION'"));
        assertTrue(sql.contains("'USER_OVERRIDE_ALLOWED'"));
        assertTrue(sql.contains("'FALLBACK_TO_ADMIN'"));
        assertTrue(sql.contains("${description}"));
        assertTrue(sql.contains("COALESCE(UPDATED_BY, 'migration') = 'migration'"));
        assertTrue(sql.contains("CREATED_BY = 'migration'"));
        assertTrue(sql.contains("COALESCE(CONFIGURATION_VERSION, 1) <= 1"));
        assertFalse(sql.contains("SYSTEM_PROMPT IS NULL"));
        assertFalse(sql.contains("btrim(SYSTEM_PROMPT) = ''"));
        assertFalse(sql.contains("SYSTEM_PROMPT !~"));
        assertTrue(sql.contains("E'Описание вакансии для обработки:\\n\\n${description}'"));
        assertFalse(sql.contains("E'Описание вакансии для обработки:\\\\n\\\\n${description}'"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertFalse(sql.contains("/Users/"));
        assertTrue(xml.contains(migration + ".sql"));
        assertTrue(xml.contains("tableName=\"HUNTTECH_AI_FUNCTION_CONFIGURATION\""));
        assertFalse(xml.contains("tableName=\"HUNTECH_AI_FUNCTION_CONFIGURATION\""));
        assertTrue(master.contains(migration + ".xml"));

        String marker = "$vacancy_prompt$";
        int start = sql.indexOf(marker) + marker.length();
        if (sql.charAt(start) == '\n') {
            start++;
        }
        int end = sql.indexOf(marker, start);
        String embeddedPrompt = sql.substring(start, end);
        assertEquals(expectedPromptSha256, sha256(embeddedPrompt));

        int secondMarker = sql.indexOf(marker, end + marker.length());
        assertTrue("INSERT prompt block is missing", secondMarker >= 0);
        int secondStart = secondMarker + marker.length();
        if (sql.charAt(secondStart) == '\n') {
            secondStart++;
        }
        int secondEnd = sql.indexOf(marker, secondStart);
        assertTrue("INSERT prompt block is not closed", secondEnd > secondStart);
        assertEquals(expectedPromptSha256, sha256(sql.substring(secondStart, secondEnd)));
    }

    private String readUtf8(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
