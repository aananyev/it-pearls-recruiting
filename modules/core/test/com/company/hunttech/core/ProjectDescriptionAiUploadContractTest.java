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
 * Статический контракт upload → ProjectAiService → AI Control Plane.
 * Защищает экран от возврата к прямому provider/prompt routing.
 */
public class ProjectDescriptionAiUploadContractTest {

    @Test
    public void projectEditBuildsAutomaticDescriptionUploadWithoutXmlRewrite() throws IOException {
        String xml = read("modules/web/src/com/company/hunttech/web/screens/project/project-edit.xml");
        String java = read("modules/web/src/com/company/hunttech/web/screens/project/ProjectEdit.java");

        assertTrue(xml.contains("id=\"projectDescriptionCard\""));
        assertTrue(xml.contains("id=\"projectDescriptionRichTextArea\""));
        assertFalse("XML-контракт не должен дублировать динамический upload",
                xml.contains("id=\"projectDescriptionUpload\""));
        assertTrue(java.contains("uiComponents.create(FileUploadField.class)"));
        assertTrue(java.contains("setPermittedExtensions"));
        assertTrue(java.contains(".pdf\", \".docx\", \".txt"));
        assertTrue(java.contains("PROJECT_DESCRIPTION_UPLOAD_LIMIT"));
        assertTrue(java.contains("ProjectDescriptionTextExtractor.extract"));
        assertTrue(java.contains("runProjectDescriptionAi"));
        assertTrue(java.contains("BackgroundWorker"));
        assertTrue(java.contains("projectAiService.processUploadedDescription"));
        assertFalse(java.contains("AIProviderRegistry"));
        assertFalse(java.contains("UserAiConfiguration"));
        assertFalse(java.contains("VacancyPromptTemplate"));
    }

    @Test
    public void projectAiFacadeIsExposedToWeb() throws IOException {
        String webSpring = read("modules/web/src/com/company/hunttech/web-spring.xml");
        assertTrue(webSpring.contains("key=\"hunttech_ProjectAiService\""));
        assertTrue(webSpring.contains("value=\"com.company.hunttech.service.ProjectAiService\""));
    }

    @Test
    public void productionSeedIsIdempotentAndNonDestructive() throws IOException {
        String sql = read("modules/core/db/update/postgres/26/260812-2-addProjectDescriptionAiFunction.sql");
        String changelog = read("modules/core/db/changelog/260812-2-addProjectDescriptionAiFunction.xml");
        String master = read("modules/core/db/changelog/db.changelog-master.xml");

        assertTrue(sql.contains("PROJECT_DESCRIPTION_GENERATE"));
        assertTrue(sql.contains("ON CONFLICT (CODE) DO NOTHING"));
        assertTrue(changelog.contains("PROJECT_DESCRIPTION_GENERATE"));
        assertTrue(master.contains("260812-2-addProjectDescriptionAiFunction.xml"));

        // Liquibase и ручной production SQL должны сохранять одинаковые реальные
        // переводы строк в административном prompt, а не литералы backslash+n.
        assertTrue(sql.contains("E'Наименование проекта: ${projectName}\\n"));
        assertTrue(changelog.contains("E'Наименование проекта: ${projectName}\\n"));

        String upper = sql.toUpperCase();
        assertFalse(upper.contains("DROP TABLE"));
        assertFalse(upper.contains("DROP COLUMN"));
        assertFalse(upper.contains("DELETE FROM"));
        assertFalse(upper.contains("TRUNCATE"));
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
