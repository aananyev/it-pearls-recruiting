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
 * Защищает UI-контракт навигации по источнику AI и персональным подключениям.
 */
public class ExtSettingsWindowAiNavigationTest {

    @Test
    public void aiSidebarUsesClickableNavigationButtons() throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/"
                        + "ExtSettingsWindowEmailNavigation.java");

        assertTrue(controller.contains("initAiSettingsNavigation()"));
        assertTrue(controller.contains("aiSettingsNavigation.removeAll()"));
        assertTrue(controller.contains("aiSettingsSourceNav"));
        assertTrue(controller.contains("aiSettingsConnectionsNav"));
        assertTrue(controller.contains("aiSettingsSourceSection"));
        assertTrue(controller.contains("aiSettingsConnectionsSection"));
    }

    @Test
    public void aiNavigationFocusesCorrespondingRightSideBlock() throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/"
                        + "ExtSettingsWindowEmailNavigation.java");

        assertTrue(controller.contains("preferPersonalAiApiSettingsField.focus()"));
        assertTrue(controller.contains("aiConfigsTable.focus()"));
        assertTrue(controller.contains("ACTIVE_AI_NAVIGATION_STYLE"));
        assertTrue(controller.contains("updateAiNavigationStyles(aiSettingsSourceNav)"));
        assertTrue(controller.contains("updateAiNavigationStyles(aiSettingsConnectionsNav)"));
    }

    @Test
    public void aiNavigationDoesNotChangeConfigurationBusinessLogic() throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/"
                        + "ExtSettingsWindowEmailNavigation.java");

        assertFalse(controller.contains("preferPersonalAiApiSettingsField.setValue"));
        assertFalse(controller.contains("preferPersonalPromptsField.setValue"));
        assertFalse(controller.contains("aiConfigsTable.setSelected"));
        assertFalse(controller.contains("onAiConfigsCreateBtnClick()"));
        assertFalse(controller.contains("onAiConfigsEditBtnClick()"));
        assertFalse(controller.contains("onAiConfigsRemoveBtnClick()"));
        assertFalse(controller.contains("onAiConfigsTestBtnClick()"));
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
