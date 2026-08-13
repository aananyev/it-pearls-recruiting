package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Статический контракт миграции административных prompt для Project AI.
 *
 * Защищает локальную миграцию от изменения credential/model/policy и от
 * перезаписи уже настроенного администратором русского prompt.
 */
public class ProjectDescriptionAiPromptMigrationTest {

    private static final String CHANGELOG =
            "modules/core/db/changelog/260813-1-localizeProjectDescriptionAiPrompts.xml";
    private static final String SQL =
            "modules/core/db/update/postgres/26/260813-1-localizeProjectDescriptionAiPrompts.sql";
    private static final String MASTER =
            "modules/core/db/changelog/db.changelog-master.xml";

    @Test
    public void migrationSeedsCanonicalRussianProjectPromptsSafely() throws IOException {
        String changelog = read(CHANGELOG);
        String sql = read(SQL);
        String master = read(MASTER);

        for (String source : new String[]{changelog, sql}) {
            assertTrue(source.contains("PROJECT_DESCRIPTION_GENERATE"));
            assertTrue(source.contains("Ты — AI-редактор описаний ИТ-проектов HRM HuntTech"));
            assertTrue(source.contains("Результат должен быть на русском языке"));
            assertTrue(source.contains("Требования к результату:"));
            assertTrue(source.contains("${projectName}"));
            assertTrue(source.contains("${sourceFileName}"));
            assertTrue(source.contains("${sourceText}"));

            // Обновление разрешено для пустого/нерусского или исходного migration-seed,
            // но не должно безусловно перетирать рабочий русский prompt администратора.
            assertTrue(source.contains("SYSTEM_PROMPT !~ '[А-Яа-яЁё]'"));
            assertTrue(source.contains("PROMPT_TEMPLATE !~ '[А-Яа-яЁё]'"));
            assertTrue(source.contains("CREATED_BY = 'migration'"));
            assertTrue(source.contains("CONFIGURATION_VERSION"));

            String upper = source.toUpperCase();
            assertFalse(upper.contains("DELETE FROM"));
            assertFalse(upper.contains("DROP TABLE"));
            assertFalse(upper.contains("DROP COLUMN"));
            assertFalse(upper.contains("TRUNCATE"));
        }

        // Миграция prompt не имеет права менять routing/credentials/model существующей функции.
        String updateBlock = between(sql,
                "UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION",
                "INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION");
        assertFalse(updateBlock.contains("ADMIN_CONFIGURATION_ID"));
        assertFalse(updateBlock.contains("ADMIN_MODEL_NAME"));
        assertFalse(updateBlock.contains("API_KEY"));
        assertFalse(updateBlock.contains("EXECUTION_POLICY ="));
        assertFalse(updateBlock.contains("FALLBACK_POLICY ="));

        assertTrue(master.contains("260813-1-localizeProjectDescriptionAiPrompts.xml"));
    }

    private String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertTrue("Начало секции не найдено: " + start, startIndex >= 0);
        assertTrue("Конец секции не найден: " + end, endIndex > startIndex);
        return source.substring(startIndex, endIndex);
    }

    private String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return root;
    }
}
