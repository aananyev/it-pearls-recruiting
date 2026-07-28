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
 * Защищает UI-контракт навигации вкладок «Обо мне» и «Интерфейс».
 */
public class ExtSettingsWindowRemainingNavigationTest {

    @Test
    public void aboutMeSidebarUsesSixClickableNavigationButtons() throws IOException {
        String controller = readController();

        assertTrue(controller.contains("initUserAiProfileNavigation()"));
        assertTrue(controller.contains("userAiProfileSectionNavigation.removeAll()"));
        assertTrue(controller.contains("userAiProfileProfessionalNav"));
        assertTrue(controller.contains("userAiProfileRecruitingNav"));
        assertTrue(controller.contains("userAiProfileResponseNav"));
        assertTrue(controller.contains("userAiProfileGoalsNav"));
        assertTrue(controller.contains("userAiProfilePrivacyNav"));
        assertTrue(controller.contains("userAiProfilePreviewNav"));
    }

    @Test
    public void aboutMeNavigationSelectsExactlyOneAccordionSection() throws IOException {
        String controller = readController();

        assertTrue(controller.contains("professionalProfileGroup.setExpanded"));
        assertTrue(controller.contains("recruitingProfileGroup.setExpanded"));
        assertTrue(controller.contains("responsePreferencesGroup.setExpanded"));
        assertTrue(controller.contains("goalsGroup.setExpanded"));
        assertTrue(controller.contains("privacyGroup.setExpanded"));
        assertTrue(controller.contains("previewGroup.setExpanded"));
        assertTrue(controller.contains("currentPositionField::focus"));
        assertTrue(controller.contains("recruitingSpecializationsField::focus"));
        assertTrue(controller.contains("preferredLanguageField::focus"));
        assertTrue(controller.contains("professionalGoalsField::focus"));
        assertTrue(controller.contains("profileEnabledField::focus"));
        assertTrue(controller.contains("aiContextPreviewArea::focus"));
    }

    @Test
    public void interfaceSidebarNavigatesToExistingLegacyFields() throws IOException {
        String controller = readController();

        assertTrue(controller.contains("initInterfaceSettingsNavigation()"));
        assertTrue(controller.contains("interfaceSettingsNavigation.removeAll()"));
        assertTrue(controller.contains("interfaceSettingsWindowNav"));
        assertTrue(controller.contains("interfaceSettingsAppearanceNav"));
        assertTrue(controller.contains("interfaceSettingsRegionalNav"));
        assertTrue(controller.contains("interfaceSettingsStartupNav"));
        assertTrue(controller.contains("modeOptions.focus()"));
        assertTrue(controller.contains("appThemeField.focus()"));
        assertTrue(controller.contains("appLangField.focus()"));
        assertTrue(controller.contains("defaultScreenField.focus()"));
    }

    @Test
    public void extSettingsWindowUsesSharedEditStyleApiAlongsideLegacyClasses() throws IOException {
        String descriptor = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/"
                        + "ext-settings-window.xml");
        String controller = readController();

        assertTrue(descriptor.contains("stylename=\"user-ai-profile-navigation label-navigation\""));
        assertTrue(descriptor.contains("stylename=\"settings-section-navigation label-navigation\""));
        assertTrue(descriptor.contains("stylename=\"ai-settings-navigation label-navigation\""));
        assertTrue(descriptor.contains("label-nav-title"));
        assertTrue(descriptor.contains("label-nav-item label-nav-item-active"));
        assertTrue(descriptor.contains("edit-sidebar"));
        assertTrue(descriptor.contains("edit-toolbar"));
        assertTrue(descriptor.contains("edit-card"));
        assertTrue(descriptor.contains("edit-accordion-section"));

        assertTrue(controller.contains("settings-section-nav-item label-nav-item"));
        assertTrue(controller.contains("ai-settings-nav-item label-nav-item"));
        assertTrue(controller.contains("user-ai-profile-nav-item label-nav-item"));
        assertTrue(controller.contains("label-nav-item-active"));
    }

    @Test
    public void remainingNavigationDoesNotChangeSettingsBusinessLogic() throws IOException {
        String controller = readController();

        assertFalse(controller.contains("currentPositionField.setValue"));
        assertFalse(controller.contains("profileEnabledField.setValue"));
        assertFalse(controller.contains("modeOptions.setValue"));
        assertFalse(controller.contains("appThemeField.setValue"));
        assertFalse(controller.contains("appLangField.setValue"));
        assertFalse(controller.contains("defaultScreenField.setValue"));
        assertFalse(controller.contains("clearAiProfile()"));
        assertFalse(controller.contains("commit()"));
    }

    private String readController() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/"
                        + "ExtSettingsWindowEmailNavigation.java");
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
