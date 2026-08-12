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
 * Статический контракт новых Browse/Edit-форм AI Control Plane.
 *
 * Защищает shared Edit layout, Data View Integrity для секретов, row scope
 * пользовательских override и отсутствие plaintext binding корпоративного API key.
 */
public class AiControlPlaneScreenContractTest {
    private static final String AI_FUNCTION_EDIT =
            "modules/web/src/com/company/hunttech/web/screens/aifunctionconfiguration/ai-function-configuration-edit.xml";
    private static final String ADMIN_BROWSE =
            "modules/web/src/com/company/hunttech/web/screens/adminaiconfiguration/admin-ai-configuration-browse.xml";
    private static final String ADMIN_EDIT =
            "modules/web/src/com/company/hunttech/web/screens/adminaiconfiguration/admin-ai-configuration-edit.xml";
    private static final String OVERRIDE_BROWSE =
            "modules/web/src/com/company/hunttech/web/screens/useraifunctionoverride/user-ai-function-override-browse.xml";
    private static final String OVERRIDE_EDIT =
            "modules/web/src/com/company/hunttech/web/screens/useraifunctionoverride/user-ai-function-override-edit.xml";
    private static final String VIEWS =
            "modules/global/src/com/company/hunttech/ai-control-plane-views.xml";

    @Test
    public void allEditFormsUseSharedContract() throws IOException {
        for (String path : new String[]{AI_FUNCTION_EDIT, ADMIN_EDIT, OVERRIDE_EDIT}) {
            String xml = readProjectFile(path);
            assertTrue(path, xml.contains("stylename=\"edit-screen-layout\""));
            assertTrue(path, xml.contains("stylename=\"edit-sidebar\""));
            assertTrue(path, xml.contains("width=\"312px\""));
            assertTrue(path, xml.contains("stylename=\"label-navigation\""));
            assertTrue(path, xml.contains("label-nav-title"));
            assertTrue(path, xml.contains("label-nav-item label-nav-item-active"));
            assertTrue(path, xml.contains("stylename=\"edit-workspace\""));
            assertTrue(path, xml.contains("stylename=\"edit-footer-actions\""));
            assertTrue(path, xml.contains("showAsPanel=\"true\""));
        }
    }

    @Test
    public void corporateSecretNeverAppearsInBrowseViewOrBoundPasswordField() throws IOException {
        String views = readProjectFile(VIEWS);
        String safeView = between(views,
                "name=\"admin-ai-configuration-browse-view\"",
                "name=\"admin-ai-configuration-edit-view\"");
        String browse = readProjectFile(ADMIN_BROWSE);
        String edit = readProjectFile(ADMIN_EDIT);

        assertFalse(safeView.contains("apiKeyEncrypted"));
        assertFalse(browse.contains("property=\"apiKeyEncrypted\""));
        assertTrue(browse.contains("exclude=\"apiKeyEncrypted\""));
        assertTrue(edit.contains("<passwordField id=\"apiKeyInput\""));
        assertFalse(edit.contains("<passwordField id=\"apiKeyInput\" property="));
    }

    @Test
    public void personalOverridesAreScopedAndAdminOnlyFunctionsExcluded() throws IOException {
        String browse = readProjectFile(OVERRIDE_BROWSE);
        String edit = readProjectFile(OVERRIDE_EDIT);

        assertTrue(browse.contains("e.user = :user"));
        assertTrue(edit.contains("e.user = :user and e.isActive = true"));
        assertTrue(edit.contains("e.executionPolicy &lt;&gt; 'ADMIN_ONLY'")
                || edit.contains("e.executionPolicy <> 'ADMIN_ONLY'"));
        assertTrue(edit.contains("view=\"user-ai-configuration-override-picker-view\""));
        assertFalse(edit.contains("property=\"apiKey\""));
    }

    @Test
    public void aiMenuContainsOnlyAiControlPlaneAdditions() throws IOException {
        String menu = readProjectFile("modules/web/src/com/company/hunttech/web-menu.xml");

        assertTrue(menu.contains("screen=\"hunttech_AiFunctionConfiguration.browse\""));
        assertTrue(menu.contains("screen=\"hunttech_AdminAiConfiguration.browse\""));
        assertTrue(menu.contains("screen=\"hunttech_UserAiFunctionOverride.browse\""));
        assertTrue(menu.contains("screen=\"hunttech_VacancyPromptTemplate.browse\""));
        assertTrue(menu.contains("screen=\"hunttech_UserAiConfiguration.browse\""));
    }

    private String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertTrue("Начало секции не найдено: " + start, startIndex >= 0);
        assertTrue("Конец секции не найден: " + end, endIndex > startIndex);
        return source.substring(startIndex, endIndex);
    }

    private String readProjectFile(String relativePath) throws IOException {
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
