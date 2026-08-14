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
 * Контракт seed-миграции AI-функции PROJECT_LOGO_IMAGE_GENERATE.
 *
 * <p>Статическая проверка по образцу ProjectDescriptionAiPromptMigrationTest: миграция
 * обязана быть INSERT-only и идемпотентной, capability — IMAGE_GENERATION, промпты — на
 * русском с ожидаемыми placeholders, модель для редактирования изображений — image-модель
 * провайдера. Production prompt/policy администратора не перезаписываются.</p>
 */
public class ProjectLogoAiFunctionSeedContractTest {

    private static final String CODE = "PROJECT_LOGO_IMAGE_GENERATE";

    @Test
    public void seedIsInsertOnlyAndIdempotent() throws IOException {
        String sql = readProjectFile(
                "modules/core/db/update/postgres/26/260813-2-addProjectLogoAiFunction.sql");
        String xml = readProjectFile(
                "modules/core/db/changelog/260813-2-addProjectLogoAiFunction.xml");

        for (String migration : new String[]{sql, xml}) {
            String upper = migration.toUpperCase(Locale.ROOT);
            assertFalse("Запрещён UPDATE в seed-миграции логотипа", upper.contains("UPDATE "));
            assertFalse("Запрещён DELETE в seed-миграции логотипа", upper.contains("DELETE "));
            assertFalse("Запрещён DROP в seed-миграции логотипа", upper.contains("DROP "));
            assertFalse("Запрещён TRUNCATE в seed-миграции логотипа", upper.contains("TRUNCATE "));
            assertTrue("INSERT-only seed обязателен", upper.contains("INSERT INTO"));
            assertTrue("Должна быть защита от повторного применения",
                    upper.contains("WHERE NOT EXISTS") || upper.contains("ON CONFLICT"));
        }
    }

    @Test
    public void seedDeclaresImageGenerationCapabilityWithRussianPrompts() throws IOException {
        String sql = readProjectFile(
                "modules/core/db/update/postgres/26/260813-2-addProjectLogoAiFunction.sql");

        assertTrue(sql.contains("'PROJECT_LOGO_IMAGE_GENERATE'"));
        assertTrue(sql.contains("'IMAGE_GENERATION'"));
        assertTrue(sql.contains("'gpt-image-2'"));
        assertTrue(sql.contains("'USER_OVERRIDE_ALLOWED'"));
        assertTrue(sql.contains("'FALLBACK_TO_ADMIN'"));
        assertTrue(sql.contains("${sourceFileName}"));
        assertTrue(sql.contains("PNG с полностью прозрачным фоном"));
        assertTrue("Промпт обязан требовать удаление фоновых полостей внутри букв",
                sql.contains("замкнутые полости внутри букв"));
        assertTrue("Промпт обязан упоминать просвет буквы «А» как фон",
                sql.contains("просвет внутри буквы «А»"));
    }

    @Test
    public void seedIsIncludedInMasterChangelog() throws IOException {
        String master = readProjectFile("modules/core/db/changelog/db.changelog-master.xml");
        assertTrue(master.contains("260813-2-addProjectLogoAiFunction.xml"));
    }

    @Test
    public void aiExecutionLayerSupportsImageGenerationCapability() throws IOException {
        String service = readProjectFile("modules/global/src/com/company/hunttech/service/AiExecutionService.java");
        String bean = readProjectFile("modules/core/src/com/company/hunttech/service/AiExecutionServiceBean.java");
        String capability = readProjectFile(
                "modules/global/src/com/company/hunttech/entity/ai/AiCapability.java");
        String provider = readProjectFile("modules/core/src/com/company/hunttech/core/ai/OpenAiProvider.java");

        assertTrue(service.contains("byte[] executeImage("));
        assertTrue(bean.contains("public byte[] executeImage("));
        assertTrue(bean.contains("validateImageCapability"));
        assertTrue(capability.contains("IMAGE_GENERATION(\"IMAGE_GENERATION\")"));
        assertTrue(provider.contains("images/edits"));
        assertTrue(provider.contains("generateImage("));
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
