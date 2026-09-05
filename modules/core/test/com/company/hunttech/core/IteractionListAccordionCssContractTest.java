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
import static org.junit.Assert.fail;

/**
 * Защищает локальный CSS-контракт IteractionListEdit: аккордеоны,
 * заголовки и элементы ввода визуально повторяют SettingsWindow.
 */
public class IteractionListAccordionCssContractTest {
    private static final List<String> THEMES = Arrays.asList(
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark");

    @Test
    public void finalOverrideMatchesSettingsWindowAccordionAndFormControls() throws IOException {
        for (String theme : THEMES) {
            String settings = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/settings-window-sections.scss");
            String iteraction = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/iteraction-list-reference-finish.scss");

            String settingsCaption = extractRule(settings, ".user-ai-profile-section .v-panel-caption");
            String iteractionCaption = extractRule(iteraction,
                    ".iteraction-list-accordion-section .v-panel-caption");

            assertSharedToken(settingsCaption, iteractionCaption, "min-height: 50px;");
            assertSharedToken(settingsCaption, iteractionCaption, "padding: 12px 16px;");
            assertSharedToken(settingsCaption, iteractionCaption, "font-size: 17px !important;");
            assertSharedToken(settingsCaption, iteractionCaption, "font-weight: 700 !important;");
            assertSharedToken(settingsCaption, iteractionCaption,
                    "background: mix($v-app-background-color, $v-panel-background-color, 68%) !important;");
            assertSharedToken(settingsCaption, iteractionCaption,
                    "border-bottom: 1px solid rgba($v-font-color, 0.15) !important;");

            assertTrue(iteraction.contains("min-height: 38px !important;"));
            assertTrue(iteraction.contains("border: 1px solid rgba($v-font-color, 0.20) !important;"));
            assertTrue(iteraction.contains("border-radius: 5px !important;"));
            assertTrue(iteraction.contains(
                    "box-shadow: 0 0 0 2px rgba($v-selection-color, 0.20) !important;"));
            assertTrue(iteraction.contains("font-size: 13px !important;"));
            assertTrue(iteraction.contains("font-size: 14px !important;"));
            assertTrue(iteraction.contains(
                    "background: mix($v-app-background-color, $v-panel-background-color, 62%) !important;"));
            assertFalse(iteraction.contains("min-height: 44px;"));
            assertFalse(iteraction.contains("padding: 16px 18px 20px !important;"));
        }
    }

    private void assertSharedToken(String expectedRule, String actualRule, String token) {
        assertTrue("SettingsWindow не содержит ожидаемый токен: " + token, expectedRule.contains(token));
        assertTrue("IteractionListEdit не повторяет токен SettingsWindow: " + token, actualRule.contains(token));
    }

    private String extractRule(String scss, String selector) {
        int selectorStart = scss.indexOf(selector);
        assertTrue("Не найден SCSS-селектор: " + selector, selectorStart >= 0);
        int openingBrace = scss.indexOf('{', selectorStart);
        assertTrue("Не найдено начало правила: " + selector, openingBrace >= 0);

        int depth = 0;
        for (int i = openingBrace; i < scss.length(); i++) {
            char symbol = scss.charAt(i);
            if (symbol == '{') {
                depth++;
            } else if (symbol == '}') {
                depth--;
                if (depth == 0) {
                    return scss.substring(selectorStart, i + 1);
                }
            }
        }
        fail("Не найдено окончание SCSS-правила: " + selector);
        return "";
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
