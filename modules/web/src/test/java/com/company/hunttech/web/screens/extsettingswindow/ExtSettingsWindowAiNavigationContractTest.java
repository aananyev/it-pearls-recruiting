package com.company.hunttech.web.screens.extsettingswindow;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Регрессионный контракт навигации персональных AI API-настроек.
 *
 * Персональные ключи должны открываться из стандартного SettingsWindow,
 * а отдельный пункт UserAiConfiguration.browse не должен возвращаться
 * в главное меню. Entity, сервисы и технические экраны при этом сохраняются.
 */
public class ExtSettingsWindowAiNavigationContractTest {

    @Test
    public void settingsScreenIsOverriddenByExtSettingsWindow() throws Exception {
        String screens = readSource("modules/web/src/com/company/hunttech/web-screens.xml");

        assertTrue(screens.contains("<screen id=\"settings\""));
        assertTrue(screens.contains("extsettingswindow/ext-settings-window.xml"));
    }

    @Test
    public void settingsWindowContainsPersonalAiConfigurationTab() throws Exception {
        String xml = readSource(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml");

        assertTrue(xml.contains("id=\"aiAccessTab\""));
        assertTrue(xml.contains("id=\"userAiConfigsDs\""));
        assertTrue(xml.contains("where e.user = :ds$extUserDs"));
        assertTrue(xml.contains("id=\"aiConfigsCreateBtn\""));
        assertTrue(xml.contains("id=\"aiConfigsEditBtn\""));
        assertTrue(xml.contains("id=\"aiConfigsRemoveBtn\""));
        assertTrue(xml.contains("id=\"aiConfigsTestBtn\""));
    }

    @Test
    public void settingsWindowReusesExistingEditorAndAiService() throws Exception {
        String source = readSource(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindow.java");

        assertTrue(source.contains("UserAiConfigurationEdit.class"));
        assertTrue(source.contains("hrmAiService.testConnection(selected)"));
        assertTrue(source.contains("entity.setUser(currentUser)"));
    }

    @Test
    public void mainMenuDoesNotContainSeparateUserAiConfigurationItem() throws Exception {
        String menu = readSource("modules/web/src/com/company/hunttech/web-menu.xml");

        assertFalse(menu.contains("screen=\"hunttech_UserAiConfiguration.browse\""));
        assertTrue(menu.contains("screen=\"hunttech_VacancyPromptTemplate.browse\""));
        assertTrue(menu.contains("screen=\"hunttech_AiPromptTemplate.browse\""));
    }

    @Test
    public void administratorStillHasAiTabInUserCard() throws Exception {
        String userEditor = readSource(
                "modules/web/src/com/company/hunttech/web/screens/extuser/ext-user-edit.xml");

        assertTrue(userEditor.contains("id=\"aiSettingsTab\""));
        assertTrue(userEditor.contains("id=\"userAiConfigsDs\""));
        assertTrue(userEditor.contains("where e.user = :ds$userDs"));
    }

    private static String readSource(String relativePath) throws Exception {
        String base = System.getProperty("user.dir");
        if (!new File(base, relativePath).exists()) {
            base = new File(base).getParent();
        }
        File file = new File(base, relativePath);
        if (!file.exists()) {
            file = new File("../../" + relativePath);
        }
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
