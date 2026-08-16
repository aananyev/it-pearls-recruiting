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
 * Контракт seed-миграции AI-функции SKILLS_EXTRACT (SkillAnalysisService).
 *
 * <p>Статическая проверка по образцу ProjectLogoAiFunctionSeedContractTest: миграция
 * обязана быть INSERT-only и идемпотентной, capability — TEXT_GENERATION, промпты — на
 * русском с ожидаемыми placeholders (${sourceText}, ${skillLevel}) и требованием
 * JSON-массива на выходе. Production prompt/policy администратора не перезаписываются.</p>
 */
public class SkillAnalysisAiFunctionSeedContractTest {

    private static final String CODE = "SKILLS_EXTRACT";
    private static final String SQL_FILE = "modules/core/db/update/postgres/26/260815-1-addSkillAnalysisAiFunction.sql";
    private static final String XML_FILE = "modules/core/db/changelog/260815-1-addSkillAnalysisAiFunction.xml";

    @Test
    public void seedIsInsertOnlyAndIdempotent() throws IOException {
        String sql = readProjectFile(SQL_FILE);
        String xml = readProjectFile(XML_FILE);

        for (String migration : new String[]{sql, xml}) {
            String upper = migration.toUpperCase(Locale.ROOT);
            assertFalse("Запрещён UPDATE в seed-миграции " + CODE, upper.contains("UPDATE "));
            assertFalse("Запрещён DELETE в seed-миграции " + CODE, upper.contains("DELETE "));
            assertFalse("Запрещён DROP в seed-миграции " + CODE, upper.contains("DROP "));
            assertFalse("Запрещён TRUNCATE в seed-миграции " + CODE, upper.contains("TRUNCATE "));
            assertTrue("INSERT-only seed обязателен", upper.contains("INSERT INTO"));
            assertTrue("Должна быть защита от повторного применения",
                    upper.contains("WHERE NOT EXISTS") || upper.contains("ON CONFLICT"));
        }
    }

    @Test
    public void seedDeclaresTextGenerationCapabilityWithRussianPrompts() throws IOException {
        String sql = readProjectFile(SQL_FILE);

        assertTrue(sql.contains("'" + CODE + "'"));
        assertTrue(sql.contains("'TEXT_GENERATION'"));
        assertTrue(sql.contains("'USER_OVERRIDE_ALLOWED'"));
        assertTrue(sql.contains("'FALLBACK_TO_ADMIN'"));
        assertTrue("Промпт-шаблон обязан использовать ${sourceText}",
                sql.contains("${sourceText}"));
        assertTrue("Промпт-шаблон обязан использовать ${skillLevel}",
                sql.contains("${skillLevel}"));
        assertTrue("Промпт обязан требовать JSON-массив на выходе",
                sql.contains("JSON-массив"));
        assertTrue("Промпт обязан описывать уровни анализа",
                sql.contains("MAIN") && sql.contains("SECONDARY") && sql.contains("TERTIARY"));
        assertTrue("Промпт обязан запрещать выдумывание навыков",
                sql.contains("Не выдумывай навыков"));
    }

    @Test
    public void seedIsIncludedInMasterChangelog() throws IOException {
        String master = readProjectFile("modules/core/db/changelog/db.changelog-master.xml");
        assertTrue(master.contains(XML_FILE.replace("modules/core/db/changelog/", "")));
    }

    @Test
    public void singleExperiencePromptUpdateIsRegisteredAndSafe() throws IOException {
        String sqlFile = "modules/core/db/update/postgres/26/260816-3-updateSkillAnalysisSingleExperiencePrompt.sql";
        String xmlFile = "modules/core/db/changelog/260816-3-updateSkillAnalysisSingleExperiencePrompt.xml";
        String sql = readProjectFile(sqlFile);
        String xml = readProjectFile(xmlFile);
        String master = readProjectFile("modules/core/db/changelog/db.changelog-master.xml");

        assertTrue(master.contains(xmlFile.replace("modules/core/db/changelog/", "")));
        for (String migration : new String[]{sql, xml}) {
            assertTrue(migration.contains("'SKILLS_EXTRACT'"));
            assertTrue("Промпт обязан требовать РОВНО ОДИН навык опыта",
                    migration.contains("РОВНО ОДИН навык опыта"));
            assertTrue("Промпт обязан определять общий стаж кандидата",
                    migration.contains("общий стаж"));
            assertTrue("Миграция не должна перезаписывать админскую настройку",
                    migration.contains("COALESCE(UPDATED_BY, 'migration') = 'migration'"));
        }
    }

    @Test
    public void disjointLevelsPromptUpdateIsRegisteredAndSafe() throws IOException {
        String sqlFile = "modules/core/db/update/postgres/26/260816-5-updateSkillAnalysisDisjointLevelsPrompt.sql";
        String xmlFile = "modules/core/db/changelog/260816-5-updateSkillAnalysisDisjointLevelsPrompt.xml";
        String sql = readProjectFile(sqlFile);
        String xml = readProjectFile(xmlFile);
        String master = readProjectFile("modules/core/db/changelog/db.changelog-master.xml");

        assertTrue(master.contains(xmlFile.replace("modules/core/db/changelog/", "")));
        for (String migration : new String[]{sql, xml}) {
            assertTrue(migration.contains("'SKILLS_EXTRACT'"));
            assertTrue("Промпт обязан относить каждый навык ровно к одному уровню",
                    migration.contains("РОВНО к одному уровню"));
            assertTrue("Промпт обязан запрещать дублирование навыков между уровнями",
                    migration.contains("НЕ дублируй навыки между уровнями"));
            assertTrue("Промпт обязан сохранять требование единственного навыка опыта",
                    migration.contains("РОВНО ОДИН навык опыта"));
            assertTrue("Промпт обязан сохранять JSON-массив на выходе",
                    migration.contains("JSON-массив"));
            assertTrue("Миграция не должна перезаписывать админскую настройку",
                    migration.contains("COALESCE(UPDATED_BY, 'migration') = 'migration'"));
        }
    }

    @Test
    public void serviceInterfaceDeclaresFourAnalysisMethodsAndFunctionCode() throws IOException {
        String service = readProjectFile(
                "modules/global/src/com/company/hunttech/service/SkillAnalysisService.java");

        assertTrue(service.contains("FUNCTION_SKILLS_EXTRACT = \"SKILLS_EXTRACT\""));
        assertTrue(service.contains("SkillAnalysisResult analyzeAll(String sourceText)"));
        assertTrue(service.contains("SkillAnalysisResult analyzeMain(String sourceText)"));
        assertTrue(service.contains("SkillAnalysisResult analyzeSecondary(String sourceText)"));
        assertTrue(service.contains("SkillAnalysisResult analyzeTertiary(String sourceText)"));
    }

    @Test
    public void serviceIsRegisteredInWebProxy() throws IOException {
        String webSpring = readProjectFile("modules/web/src/com/company/hunttech/web-spring.xml");
        assertTrue(webSpring.contains("hunttech_SkillAnalysisService"));
        assertTrue(webSpring.contains("com.company.hunttech.service.SkillAnalysisService"));
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
