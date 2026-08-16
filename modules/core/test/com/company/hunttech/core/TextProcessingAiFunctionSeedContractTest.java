package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Контракт AI-функций умного форматирования текста (TextProcessingService):
 * seed-миграция TEXT_SMART_FORMAT_HTML/TEXT_SMART_FORMAT_PLAIN и модернизация
 * SYSTEM_PROMPT TEXT_SMART_FORMAT_HTML (260816-4) с требованием удалять пустые
 * строки и пустые абзацы из итогового HTML.
 *
 * <p>Статическая проверка по образцу SkillAnalysisAiFunctionSeedContractTest:
 * seed обязан быть INSERT-only и идемпотентным; update-миграция не перезаписывает
 * промпт, изменённый администратором (контракт 260814-3).</p>
 */
public class TextProcessingAiFunctionSeedContractTest {

    private static final String HTML_CODE = "TEXT_SMART_FORMAT_HTML";
    private static final String PLAIN_CODE = "TEXT_SMART_FORMAT_PLAIN";
    private static final String SEED_SQL = "modules/core/db/update/postgres/26/260816-1-addTextProcessingAiFunction.sql";
    private static final String SEED_XML = "modules/core/db/changelog/260816-1-addTextProcessingAiFunction.xml";
    private static final String UPDATE_SQL = "modules/core/db/update/postgres/26/260816-4-updateTextProcessingHtmlPrompt.sql";
    private static final String UPDATE_XML = "modules/core/db/changelog/260816-4-updateTextProcessingHtmlPrompt.xml";

    @Test
    public void seedIsInsertOnlyAndIdempotent() throws IOException {
        String sql = readProjectFile(SEED_SQL);
        String xml = readProjectFile(SEED_XML);

        for (String migration : new String[]{sql, xml}) {
            String upper = migration.toUpperCase(Locale.ROOT);
            assertFalse("Запрещён UPDATE в seed-миграции TextProcessing", upper.contains("UPDATE "));
            assertFalse("Запрещён DELETE в seed-миграции TextProcessing", upper.contains("DELETE "));
            assertFalse("Запрещён DROP в seed-миграции TextProcessing", upper.contains("DROP "));
            assertTrue("INSERT-only seed обязателен", upper.contains("INSERT INTO"));
            assertTrue("Должна быть защита от повторного применения",
                    upper.contains("WHERE NOT EXISTS") || upper.contains("ON CONFLICT"));
        }
    }

    @Test
    public void seedDeclaresTextGenerationCapabilityWithRussianPrompts() throws IOException {
        String sql = readProjectFile(SEED_SQL);

        assertTrue(sql.contains("'" + HTML_CODE + "'"));
        assertTrue(sql.contains("'" + PLAIN_CODE + "'"));
        assertTrue(sql.contains("'TEXT_GENERATION'"));
        assertTrue(sql.contains("'USER_OVERRIDE_ALLOWED'"));
        assertTrue(sql.contains("'FALLBACK_TO_ADMIN'"));
        assertTrue("Промпт-шаблон обязан использовать ${sourceText}",
                sql.contains("${sourceText}"));
        assertTrue("HTML-промпт обязан требовать чистое оформление секций",
                sql.contains("<ul><li>"));
        assertTrue("Plain-промпт обязан структурировать секции",
                sql.contains("разделителями"));
    }

    @Test
    public void seedIsIncludedInMasterChangelog() throws IOException {
        String master = readProjectFile("modules/core/db/changelog/db.changelog-master.xml");
        assertTrue(master.contains(SEED_XML.replace("modules/core/db/changelog/", "")));
    }

    @Test
    public void htmlPromptUpdateRequiresRemovingEmptyLinesAndIsRegistered() throws IOException {
        String sql = readProjectFile(UPDATE_SQL);
        String xml = readProjectFile(UPDATE_XML);
        String master = readProjectFile("modules/core/db/changelog/db.changelog-master.xml");

        assertTrue(master.contains(UPDATE_XML.replace("modules/core/db/changelog/", "")));

        for (String migration : new String[]{sql, xml}) {
            assertTrue(migration.contains("'" + HTML_CODE + "'"));
            assertTrue("Промпт обязан требовать УДАЛЕНИЕ пустых строк из текста",
                    migration.contains("УДАЛИ из исходного текста все пустые строки"));
            assertTrue("Промпт обязан запрещать пустые абзацы <p></p> в итоговом HTML",
                    migration.contains("пустых <p></p>"));
            assertTrue("Промпт обязан запрещать пустые элементы списка <li>",
                    migration.contains("пустых <li>"));
            assertTrue("Миграция не должна перезаписывать админскую настройку",
                    migration.contains("COALESCE(UPDATED_BY, 'migration') = 'migration'"));
            assertTrue("Для свежих сред обязан быть INSERT с новой версией промпта",
                    migration.contains("WHERE NOT EXISTS") || migration.contains("ON CONFLICT"));
        }
    }

    private String readProjectFile(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
