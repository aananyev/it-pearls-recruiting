package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает локальный CSS-контракт аккордеонов IteractionListEdit,
 * визуально синхронизированный с подтверждённым оформлением SettingsWindow.
 */
public class IteractionListAccordionCssContractTest {

    private static final List<String> THEMES = Arrays.asList(
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark");

    @Test
    public void allThemesKeepSettingsWindowGeometryInsideIteractionListRoot() throws IOException {
        for (String theme : THEMES) {
            String scss = readProjectFile(
                    "modules/web/themes/" + theme
                            + "/com.company.hunttech/iteraction-list-accordion-navigation.scss");
            String settingsScss = readProjectFile(
                    "modules/web/themes/" + theme
                            + "/com.company.hunttech/user-ai-profile.scss");
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");

            assertTrue("В теме " + theme + " отсутствует локальный root IteractionListEdit",
                    scss.contains(".iteraction-list-editor"));
            assertTrue("В теме " + theme + " не оформлен фактический класс аккордеона",
                    scss.contains(".user-ai-profile-section"));
            assertTrue("В теме " + theme + " не сохранён локальный accordion-класс",
                    scss.contains(".iteraction-list-accordion-section"));

            assertCanonicalRule(theme, scss, settingsScss, "margin-bottom: 10px;");
            assertCanonicalRule(theme, scss, settingsScss, "border-radius: 7px");
            assertCanonicalRule(theme, scss, settingsScss,
                    "border: 1px solid rgba(127, 127, 127, 0.20)");
            assertCanonicalRule(theme, scss, settingsScss,
                    "background: rgba(255, 255, 255, 0.45)");
            assertCanonicalRule(theme, scss, settingsScss, "padding-top: 9px;");
            assertCanonicalRule(theme, scss, settingsScss, "padding-bottom: 9px;");
            assertCanonicalRule(theme, scss, settingsScss, "font-weight: 600");
            assertCanonicalRule(theme, scss, settingsScss,
                    "background: rgba(127, 127, 127, 0.045)");

            assertFalse("В теме " + theme + " запрещён глобальный panel-selector",
                    scss.contains("@mixin iteraction-list-accordion-navigation-theme {\n  .v-panel"));
            assertTrue(styles.contains(
                    "@import \"com.company.hunttech/iteraction-list-accordion-navigation\";"));
            assertTrue(styles.contains("@include iteraction-list-accordion-navigation-theme;"));
        }
    }

    private void assertCanonicalRule(String theme,
                                     String iteractionScss,
                                     String settingsScss,
                                     String rule) {
        assertTrue("В эталоне SettingsWindow темы " + theme + " отсутствует правило: " + rule,
                settingsScss.contains(rule));
        assertTrue("В IteractionListEdit темы " + theme + " отсутствует правило: " + rule,
                iteractionScss.contains(rule));
    }

    private String readProjectFile(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8);
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
