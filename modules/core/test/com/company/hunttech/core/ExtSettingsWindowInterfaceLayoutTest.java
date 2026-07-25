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
 * Защищает presentation-only контракт выровненной вкладки «Интерфейс».
 */
public class ExtSettingsWindowInterfaceLayoutTest {

    @Test
    public void extensionDescriptorUsesInterfaceLayoutController() throws IOException {
        String descriptor = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/"
                        + "ext-settings-window-email-navigation.xml");

        assertTrue(descriptor.contains(
                "class=\"com.company.hunttech.web.screens.extsettingswindow."
                        + "ExtSettingsWindowInterfaceLayout\""));
        assertTrue(descriptor.contains(
                "extends=\"/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml\""));
    }

    @Test
    public void interfaceRowsUseUnifiedGeometry() throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/"
                        + "ExtSettingsWindowInterfaceLayout.java");

        assertTrue(controller.contains("INTERFACE_LABEL_WIDTH = \"190px\""));
        assertTrue(controller.contains("modeOptions.setOrientation(HasOrientation.Orientation.HORIZONTAL)"));
        assertTrue(controller.contains("appThemeField.setWidth(INTERFACE_CONTROL_WIDTH)"));
        assertTrue(controller.contains("appLangField.setWidth(INTERFACE_CONTROL_WIDTH)"));
        assertTrue(controller.contains("defaultScreenField.setWidth(INTERFACE_CONTROL_WIDTH)"));
        assertTrue(controller.contains("timeZoneBox.resetExpanded()"));
        assertTrue(controller.contains("timeZoneBox.expand(timeZoneLookup)"));
        assertTrue(controller.contains("timeZoneAutoField.setWidth(AUTO_TIME_ZONE_WIDTH)"));
    }

    @Test
    public void layoutControllerDoesNotChangeSettingsValuesOrActions() throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/"
                        + "ExtSettingsWindowInterfaceLayout.java");

        assertFalse(controller.contains("setValue("));
        assertFalse(controller.contains("commit("));
        assertFalse(controller.contains("changePassword"));
        assertFalse(controller.contains("resetScreenSettings"));
        assertFalse(controller.contains("DataManager"));
        assertFalse(controller.contains("UserSettings"));
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
