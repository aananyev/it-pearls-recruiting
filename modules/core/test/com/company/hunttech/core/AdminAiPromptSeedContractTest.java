package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Контракт консолидированной миграции административных AI-промптов 260818-1.
 *
 * <p>Статическая проверка (без контейнера, только чтение файлов): миграция обязана
 * загружать ВСЕ канонические административные промпты AI-функций, используемых кодом
 * (PROJECT_DESCRIPTION_GENERATE, PROJECT_LOGO_IMAGE_GENERATE,
 * PROJECT_SHORT_DESCRIPTION_GENERATE, SKILLS_EXTRACT, TEXT_SMART_FORMAT_HTML,
 * TEXT_SMART_FORMAT_PLAIN, STANDARDIZE_VACANCY), быть идемпотентной и НЕ перезаписывать
 * административную настройку (контракт 260814-3/260816-5). Обе копии (.sql и .xml)
 * обязаны быть синхронны по ключевым требованиям промптов.</p>
 */
public class AdminAiPromptSeedContractTest {

    private static final String SQL_FILE =
            "modules/core/db/update/postgres/26/260818-1-addAdminAiPromptSeed.sql";
    private static final String XML_FILE =
            "modules/core/db/changelog/260818-1-addAdminAiPromptSeed.xml";

    /** CODE → ключевые требования промпта, которые обязаны присутствовать в ОБЕИХ копиях. */
    private static final Map<String, String[]> REQUIRED_PHRASES = new LinkedHashMap<>();

    static {
        REQUIRED_PHRASES.put("PROJECT_DESCRIPTION_GENERATE",
                new String[]{"Ты — AI-редактор описаний ИТ-проектов", "${sourceText}"});
        REQUIRED_PHRASES.put("PROJECT_LOGO_IMAGE_GENERATE",
                new String[]{"удали ВСЕ однотонные фоновые области", "IMAGE_GENERATION"});
        REQUIRED_PHRASES.put("PROJECT_SHORT_DESCRIPTION_GENERATE",
                new String[]{"не более 2 предложений", "MAX_TOKENS = 250"});
        REQUIRED_PHRASES.put("SKILLS_EXTRACT",
                new String[]{"РОВНО ОДИН навык опыта", "НЕ дублируй навыки между уровнями",
                        "JSON-массив", "${skillLevel}"});
        REQUIRED_PHRASES.put("TEXT_SMART_FORMAT_HTML",
                new String[]{"УДАЛИ из исходного текста все пустые строки", "${sourceText}"});
        REQUIRED_PHRASES.put("TEXT_SMART_FORMAT_PLAIN",
                new String[]{"аккуратный plain text", "${sourceText}"});
        REQUIRED_PHRASES.put("STANDARDIZE_VACANCY",
                new String[]{"НЕТ ДАННЫХ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ",
                        "14 пунктов", "TEXT_TRANSFORMATION", "USER_REQUIRED", "NO_FALLBACK",
                        "${rawDescription}"});
    }

    @Test
    public void allAdminPromptsSeededInBothCopies() throws IOException {
        String sql = readProjectFile(SQL_FILE);
        String xml = readProjectFile(XML_FILE);

        assertTrue("SQL обязан содержать 7 функций", sql.contains("'PROJECT_DESCRIPTION_GENERATE'"));
        assertTrue("XML обязан содержать 7 функций", xml.contains("'PROJECT_DESCRIPTION_GENERATE'"));
        for (Map.Entry<String, String[]> entry : REQUIRED_PHRASES.entrySet()) {
            assertTrue("CODE " + entry.getKey() + " отсутствует в SQL",
                    sql.contains("'" + entry.getKey() + "'"));
            assertTrue("CODE " + entry.getKey() + " отсутствует в XML",
                    xml.contains("'" + entry.getKey() + "'"));
            for (String phrase : entry.getValue()) {
                assertTrue("Требование «" + phrase + "» функции " + entry.getKey()
                        + " отсутствует в SQL", sql.contains(phrase));
                assertTrue("Требование «" + phrase + "» функции " + entry.getKey()
                        + " отсутствует в XML", xml.contains(phrase));
            }
        }
    }

    @Test
    public void adminConfigurationIsNeverOverwritten() throws IOException {
        String sql = readProjectFile(SQL_FILE);
        String xml = readProjectFile(XML_FILE);

        for (String migration : new String[]{sql, xml}) {
            assertTrue("Обе копии обязаны защищать админскую настройку",
                    migration.contains("COALESCE(UPDATED_BY, 'migration') = 'migration'"));
            assertTrue("UPDATE обязан ограничиваться migration-owned записями",
                    migration.contains("CREATED_BY = 'migration'"));
        }
    }

    @Test
    public void seedIsIdempotentAndNonDestructive() throws IOException {
        String sql = readProjectFile(SQL_FILE);
        String xml = readProjectFile(XML_FILE);

        for (String migration : new String[]{sql, xml}) {
            String upper = migration.toUpperCase(java.util.Locale.ROOT);
            assertFalse("Запрещён DELETE в миграции", upper.contains("DELETE "));
            assertFalse("Запрещён DROP в миграции", upper.contains("DROP "));
            assertFalse("Запрещён TRUNCATE в миграции", upper.contains("TRUNCATE "));
            assertTrue("INSERT обязан быть идемпотентным (WHERE NOT EXISTS)",
                    upper.contains("WHERE NOT EXISTS") || upper.contains("ON CONFLICT"));
        }
    }

    @Test
    public void controlPlaneTablesAreCreatedOnlyIfMissing() throws IOException {
        String xml = readProjectFile(XML_FILE);
        String sql = readProjectFile(SQL_FILE);

        assertTrue("XML changeSet создания таблиц обязан иметь precondition",
                xml.contains("260818-1-createControlPlaneIfMissing"));
        assertTrue("XML precondition обязан быть MARK_RAN при существующей таблице",
                xml.contains("onFail=\"MARK_RAN\""));
        assertTrue("XML precondition обязан проверять отсутствие таблицы",
                xml.contains("<not>") && xml.contains("tableExists"));
        assertTrue("SQL обязан создавать таблицу только при отсутствии",
                sql.contains("IF to_regclass('public.hunttech_ai_function_configuration') IS NULL THEN"));
        assertTrue("SQL обязан быть самодостаточным (CREATE TABLE внутри скрипта)",
                sql.contains("CREATE TABLE HUNTTECH_AI_FUNCTION_CONFIGURATION"));
    }

    @Test
    public void legacyVacancyContractPreserved() throws IOException {
        String sql = readProjectFile(SQL_FILE);
        String xml = readProjectFile(XML_FILE);

        for (String migration : new String[]{sql, xml}) {
            assertTrue("STANDARDIZE_VACANCY обязан быть TEXT_TRANSFORMATION (legacy-контракт 260812-4)",
                    migration.contains("'STANDARDIZE_VACANCY'")
                            && migration.contains("'TEXT_TRANSFORMATION'"));
            assertTrue("STANDARDIZE_VACANCY обязан быть USER_REQUIRED",
                    migration.contains("'USER_REQUIRED'"));
            assertTrue("STANDARDIZE_VACANCY обязан быть NO_FALLBACK",
                    migration.contains("'NO_FALLBACK'"));
        }
    }

    @Test
    public void migrationIsRegisteredInMasterChangelog() throws IOException {
        String master = readProjectFile("modules/core/db/changelog/db.changelog-master.xml");
        assertTrue(master.contains(XML_FILE.replace("modules/core/db/changelog/", "")));
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
