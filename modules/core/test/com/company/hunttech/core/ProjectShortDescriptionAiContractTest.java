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
 * Контракт фичи «Кратко» ProjectEdit: AI-генерация краткого описания сути проекта
 * (sidebar-раздел «Коротко»). Защищает от возврата к прямому provider/prompt routing
 * и от потери связи «кнопка → ProjectAiService → AI Control Plane → поле сущности».
 */
public class ProjectShortDescriptionAiContractTest {

    private static final String CODE = "PROJECT_SHORT_DESCRIPTION_GENERATE";

    @Test
    public void entityHasShortDescriptionField() throws IOException {
        String entity = read("modules/global/src/com/company/hunttech/entity/Project.java");
        assertTrue(entity.contains("SHORT_DESCRIPTION"));
        assertTrue(entity.contains("getShortDescription()"));
        assertTrue(entity.contains("setShortDescription(String"));
    }

    @Test
    public void projectEditViewLoadsShortDescriptionEagerly() throws IOException {
        String views = read("modules/global/src/com/company/hunttech/views.xml");
        // shortDescription обязан быть в project-edit-view: sidebar-раздел «Коротко»
        // виден уже при открытии формы (до lazy load вкладки «Описание проекта»).
        int editView = views.indexOf("name=\"project-edit-view\"");
        assertTrue("project-edit-view не найден", editView >= 0);
        int propertyIdx = views.indexOf("<property name=\"shortDescription\"/>", editView);
        assertTrue("shortDescription отсутствует в project-edit-view", propertyIdx > editView);
    }

    @Test
    public void controllerWiresButtonToAiFacadeWithoutProviderRouting() throws IOException {
        String java = read("modules/web/src/com/company/hunttech/web/screens/project/ProjectEdit.java");
        String xml = read("modules/web/src/com/company/hunttech/web/screens/project/project-edit.xml");

        // Кнопка «Кратко» создаётся программно в строке upload (XML-контракт не дублирует).
        assertFalse("XML-контракт не должен дублировать динамическую кнопку «Кратко»",
                xml.contains("id=\"projectDescriptionShortButton\""));
        assertTrue("Нет кнопки «Кратко»", java.contains("projectDescriptionShortButton"));
        assertTrue("Кнопка не блокируется без текста", java.contains("setEnabled(false)"));
        assertTrue("Кнопка не подписана на клик",
                java.contains("onProjectDescriptionShortButtonClick"));
        assertTrue("Нет HTML→текст конвертации", java.contains("stripHtmlToPlainText"));
        assertTrue("Нет фонового AI-вызова", java.contains("BackgroundWorker"));
        assertTrue("Нет вызова facade", java.contains("projectAiService.generateShortDescription"));
        assertTrue("Нет записи результата в сущность",
                java.contains("getEditedEntity().setShortDescription"));

        // Sidebar-раздел «Коротко»: показ/скрытие и заполнение текста контроллером.
        assertTrue("Нет управления видимостью раздела", java.contains("applyShortDescriptionSidebar"));
        assertTrue("Раздел не скрывается при пустом значении",
                java.contains("projectEditorSidebarShortDescription.setVisible"));
        assertTrue("Текст раздела не заполняется",
                java.contains("projectSidebarShortDescriptionText.setValue"));
        assertTrue("Раздел не инициализируется при открытии",
                java.contains("applyShortDescriptionSidebar(getEditedEntity().getShortDescription())"));

        // Экран не содержит provider/credential-маршрутизацию.
        assertFalse(java.contains("AIProviderRegistry"));
        assertFalse(java.contains("UserAiConfiguration"));
        assertFalse(java.contains("VacancyPromptTemplate"));
    }

    @Test
    public void aiFacadeDeclaresShortDescriptionFunction() throws IOException {
        String service = read("modules/global/src/com/company/hunttech/service/ProjectAiService.java");
        String bean = read("modules/core/src/com/company/hunttech/service/ProjectAiServiceBean.java");

        assertTrue(service.contains("PROJECT_SHORT_DESCRIPTION_GENERATE"));
        assertTrue(service.contains("generateShortDescription(String projectName, String descriptionText)"));
        assertTrue(bean.contains("generateShortDescription(String projectName, String descriptionText)"));
        assertTrue(bean.contains("executeText(\n                FUNCTION_PROJECT_SHORT_DESCRIPTION_GENERATE, context)"));
        assertTrue("Контекст без projectName",
                bean.contains("context.put(\"projectName\""));
        assertTrue("Контекст без sourceText",
                bean.contains("context.put(\"sourceText\""));
    }

    @Test
    public void seedIsInsertOnlyAndIdempotent() throws IOException {
        String sql = read("modules/core/db/update/postgres/26/260814-2-addProjectShortDescriptionAiFunction.sql");
        String changelog = read("modules/core/db/changelog/260814-2-addProjectShortDescriptionAiFunction.xml");

        for (String migration : new String[]{sql, changelog}) {
            String upper = migration.toUpperCase(Locale.ROOT);
            assertFalse("Запрещён UPDATE в seed-миграции «Кратко»", upper.contains("UPDATE "));
            assertFalse("Запрещён DELETE в seed-миграции «Кратко»", upper.contains("DELETE "));
            assertFalse("Запрещён DROP в seed-миграции «Кратко»", upper.contains("DROP "));
            assertFalse("Запрещён TRUNCATE в seed-миграции «Кратко»", upper.contains("TRUNCATE "));
            assertTrue("INSERT-only seed обязателен", upper.contains("INSERT INTO"));
            assertTrue("Должна быть защита от повторного применения",
                    upper.contains("WHERE NOT EXISTS") || upper.contains("ON CONFLICT"));
        }
    }

    @Test
    public void seedDeclaresTextGenerationCapabilityWithRussianPrompts() throws IOException {
        String sql = read("modules/core/db/update/postgres/26/260814-2-addProjectShortDescriptionAiFunction.sql");

        assertTrue(sql.contains("'" + CODE + "'"));
        assertTrue(sql.contains("'TEXT_GENERATION'"));
        assertTrue(sql.contains("'USER_OVERRIDE_ALLOWED'"));
        assertTrue(sql.contains("'FALLBACK_TO_ADMIN'"));
        assertTrue("Промпт без ограничения 1 предложения",
                sql.contains("не более 1 предложения"));
        assertTrue("Промпт без запрета выдумывать факты",
                sql.contains("Не выдумывай фактов"));
        assertTrue("MAX_TOKENS не сокращён до 125 (генерация в 4 раза короче)",
                sql.contains("0.3,\n        125,"));
        assertTrue("Промпт-шаблон без ${projectName}", sql.contains("${projectName}"));
        assertTrue("Промпт-шаблон без ${sourceText}", sql.contains("${sourceText}"));
    }

    @Test
    public void seedAndColumnAreIncludedInMasterChangelog() throws IOException {
        String master = read("modules/core/db/changelog/db.changelog-master.xml");
        assertTrue(master.contains("260814-1-addProjectShortDescriptionColumn.xml"));
        assertTrue(master.contains("260814-2-addProjectShortDescriptionAiFunction.xml"));
        assertTrue(master.contains("260814-3-shortenProjectShortDescriptionPrompt.xml"));
        assertTrue(master.contains("260814-4-increaseProjectShortDescriptionPrompt.xml"));
    }

    @Test
    public void shortenPromptMigrationUpdatesExistingSeedOnly() throws IOException {
        String sql = read("modules/core/db/update/postgres/26/260814-3-shortenProjectShortDescriptionPrompt.sql");
        String changelog = read("modules/core/db/changelog/260814-3-shortenProjectShortDescriptionPrompt.xml");

        for (String migration : new String[]{sql, changelog}) {
            String upper = migration.toUpperCase(Locale.ROOT);
            assertTrue("Нет UPDATE существующей записи", upper.contains("UPDATE "));
            assertTrue("UPDATE не ограничен по CODE",
                    migration.contains("WHERE CODE = 'PROJECT_SHORT_DESCRIPTION_GENERATE'"));
            assertFalse("Запрещён DROP в миграции", upper.contains("DROP "));
            assertFalse("Запрещён TRUNCATE в миграции", upper.contains("TRUNCATE "));
        }
        assertTrue("Нет нового SYSTEM_PROMPT (1 предложение)",
                sql.contains("не более 1 предложения"));
        assertTrue("MAX_TOKENS не 125", sql.contains("MAX_TOKENS = 125"));
        assertTrue("Нет INSERT-fallback (WHERE NOT EXISTS)",
                sql.toUpperCase(Locale.ROOT).contains("WHERE NOT EXISTS"));
        // Административная настройка не перезаписывается: только записи от
        // миграции/без кастомизации попадают под UPDATE (контракт 260813-1).
        assertTrue("Нет защиты админской настройки (CREATED_BY = 'migration')",
                sql.contains("CREATED_BY = 'migration'"));
        assertTrue("Нет защиты админской настройки (CONFIGURATION_VERSION)",
                sql.contains("CONFIGURATION_VERSION, 1) <= 1"));
    }

    @Test
    public void increasePromptMigrationDoublesGeneration() throws IOException {
        String sql = read("modules/core/db/update/postgres/26/260814-4-increaseProjectShortDescriptionPrompt.sql");
        String changelog = read("modules/core/db/changelog/260814-4-increaseProjectShortDescriptionPrompt.xml");

        for (String migration : new String[]{sql, changelog}) {
            String upper = migration.toUpperCase(Locale.ROOT);
            assertTrue("Нет UPDATE существующей записи", upper.contains("UPDATE "));
            assertTrue("UPDATE не ограничен по CODE",
                    migration.contains("WHERE CODE = 'PROJECT_SHORT_DESCRIPTION_GENERATE'"));
            assertFalse("Запрещён DROP в миграции", upper.contains("DROP "));
            assertFalse("Запрещён TRUNCATE в миграции", upper.contains("TRUNCATE "));
            assertFalse("Запрещён DELETE в миграции", upper.contains("DELETE "));
        }
        // Генерация в 2 раза больше: два предложения вместо одного, MAX_TOKENS 250.
        assertTrue("Нет нового SYSTEM_PROMPT (2 предложения)",
                sql.contains("не более 2 предложений"));
        assertTrue("MAX_TOKENS не 250", sql.contains("MAX_TOKENS = 250"));
        assertTrue("Нет INSERT-fallback (WHERE NOT EXISTS)",
                sql.toUpperCase(Locale.ROOT).contains("WHERE NOT EXISTS"));
        // Административная настройка не перезаписывается: только записи от
        // миграции (seed v1 / результат 260814-3 v2) попадают под UPDATE
        // (контракт 260813-1).
        assertTrue("Нет защиты админской настройки (CREATED_BY = 'migration')",
                sql.contains("CREATED_BY = 'migration'"));
        assertTrue("Нет защиты админской настройки (CONFIGURATION_VERSION <= 2)",
                sql.contains("CONFIGURATION_VERSION, 1) <= 2"));
        assertTrue("Промпт-шаблон без ${projectName}", sql.contains("${projectName}"));
        assertTrue("Промпт-шаблон без ${sourceText}", sql.contains("${sourceText}"));
    }

    @Test
    public void columnSqlIsIdempotentAndNonDestructive() throws IOException {
        String sql = read("modules/core/db/update/postgres/26/260814-1-addProjectShortDescriptionColumn.sql");
        String upper = sql.toUpperCase(Locale.ROOT);
        assertTrue(sql.contains("SHORT_DESCRIPTION"));
        assertTrue("Колонка должна добавляться идемпотентно",
                upper.contains("ADD COLUMN IF NOT EXISTS"));
        assertFalse("Запрещён DROP", upper.contains("DROP COLUMN"));
        assertFalse("Запрещён DELETE", upper.contains("DELETE FROM"));
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
