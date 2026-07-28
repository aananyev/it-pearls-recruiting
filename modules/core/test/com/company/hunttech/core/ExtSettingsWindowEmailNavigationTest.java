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
 * Защищает UI-контракт навигации SMTP, POP3 и IMAP во вкладке настроек email.
 */
public class ExtSettingsWindowEmailNavigationTest {

    @Test
    public void settingsScreenUsesLegacyDescriptorExtension() throws IOException {
        String extensionXml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/"
                        + "ext-settings-window-email-navigation.xml");
        String mainBackgroundXml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/"
                        + "ext-settings-window-main-background.xml");
        String screensXml = readProjectFile("modules/web/src/com/company/hunttech/web-screens.xml");

        assertTrue(extensionXml.contains(
                "extends=\"/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml\""));
        assertTrue(extensionXml.contains(
                "class=\"com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindowInterfaceLayout\""));
        assertTrue(mainBackgroundXml.contains(
                "extends=\"/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-email-navigation.xml\""));
        assertTrue(screensXml.contains(
                "id=\"settings\" template=\"/com/company/hunttech/web/screens/extsettingswindow/"
                        + "ext-settings-window-main-background.xml\""));
    }

    @Test
    public void sidebarNavigationSelectsCorrespondingAccordionSection() throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/"
                        + "ExtSettingsWindowEmailNavigation.java");

        assertTrue(controller.contains("extends ExtSettingsWindow"));
        assertTrue(controller.contains("emailSettingsNavigation.removeAll()"));
        assertTrue(controller.contains("emailSettingsSmtpNav"));
        assertTrue(controller.contains("emailSettingsPop3Nav"));
        assertTrue(controller.contains("emailSettingsImapNav"));
        assertTrue(controller.contains("smtpSettingsSection.setExpanded"));
        assertTrue(controller.contains("pop3SettingsSection.setExpanded"));
        assertTrue(controller.contains("imapSettingsSection.setExpanded"));
        assertTrue(controller.contains("selectedFirstField.focus()"));
    }

    @Test
    public void navigationExtensionDoesNotOverrideEmailBusinessLogic() throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/"
                        + "ExtSettingsWindowEmailNavigation.java");

        assertFalse(controller.contains("setEmailSettings()"));
        assertFalse(controller.contains("collectEmailSettings()"));
        assertFalse(controller.contains("protected void commit()"));
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
