package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает геометрию правой рабочей области IteractionListEdit от двух регрессий:
 * переноса workspace вниз из-за 100%-ширины Vaadin-slot и наложения строк GridLayout.
 */
public class IteractionListWorkspaceLayoutContractTest {

    private static final String[] THEMES = {
            "halo",
            "havana",
            "helium",
            "hover",
            "hunttech-modern",
            "hunttech-modern-light",
            "hunttech-modern-dark"
    };

    @Test
    public void workspaceLayoutPartialIsIdenticalAcrossAllThemes() throws IOException {
        String expected = partial("halo");

        for (String theme : THEMES) {
            assertEquals("Workspace layout должен быть идентичен: " + theme,
                    expected, partial(theme));
        }
    }

    @Test
    public void everyThemeLoadsWorkspaceLayoutAfterVisualAlignment() throws IOException {
        for (String theme : THEMES) {
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");

            assertOrdered(styles,
                    "@import \"com.company.hunttech/iteraction-list-visual-alignment\";",
                    "@import \"com.company.hunttech/iteraction-list-workspace-layout\";");
            assertOrdered(styles,
                    "@include iteraction-list-visual-alignment-theme;",
                    "@include iteraction-list-workspace-layout-theme;");
        }
    }

    @Test
    public void workspaceUsesRemainingHBoxWidthAndKeepsTopAlignment() throws IOException {
        String partial = partial("halo");

        assertTrue(partial.contains("width: calc(100% - 312px) !important;"));
        assertTrue(partial.contains("max-width: calc(100% - 312px) !important;"));
        assertTrue(partial.contains("vertical-align: top !important;"));
        assertTrue(partial.contains("width: calc(100% - 252px) !important;"));
    }

    @Test
    public void formRowsUseNormalCssGridFlowAndExplicitVerticalRhythm() throws IOException {
        String partial = partial("halo");

        // CUBA GridLayout slot-ы переводятся из absolute/relative координат
        // в нормальный CSS Grid flow, чтобы строки не могли перекрывать друг друга.
        assertTrue(partial.contains("display: grid !important;"));
        assertTrue(partial.contains("grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);"));
        assertTrue(partial.contains("row-gap: 12px;"));
        assertTrue(partial.contains("position: static !important;"));
        assertTrue(partial.contains("grid-column: 1 / span 2;"));

        // Между прямыми строками VBox используется единый 12px rhythm.
        assertTrue(partial.contains(".iteraction-list-unified-body > .v-spacing"));
        assertTrue(partial.contains("height: 12px !important;"));
    }

    private String partial(String theme) throws IOException {
        return readProjectFile("modules/web/themes/" + theme
                + "/com.company.hunttech/iteraction-list-workspace-layout.scss");
    }

    private void assertOrdered(String text, String first, String second) {
        int firstIndex = text.indexOf(first);
        int secondIndex = text.indexOf(second);
        assertTrue("Не найден первый маркер: " + first, firstIndex >= 0);
        assertTrue("Не найден второй маркер: " + second, secondIndex > firstIndex);
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
