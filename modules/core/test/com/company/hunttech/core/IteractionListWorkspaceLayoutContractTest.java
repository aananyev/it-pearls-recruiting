package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает presentation-контракт новой правой рабочей области IteractionListEdit:
 * flex workspace, четыре section cards, адаптивные grids, caption над control и
 * белую жирную подпись активных быстрых действий.
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
    public void workspaceUsesFlexForRemainingWidthAndNewSidebarV2StaysFixed() throws IOException {
        String partial = partial("halo");

        assertTrue(partial.contains("display: flex !important;"));
        assertTrue(partial.contains("flex: 1 1 0% !important;"));
        assertTrue(partial.contains("width: auto !important;"));
        assertTrue(partial.contains("min-width: 0 !important;"));
        assertTrue(partial.contains("vertical-align: top !important;"));
        assertTrue(partial.contains(".v-slot-iteraction-list-sidebar-v2"));
        assertTrue(partial.contains("flex: 0 0 auto !important;"));

        assertFalse(partial.contains("calc(100% - 312px)"));
        assertFalse(partial.contains("calc(100% - 252px)"));
        assertFalse(partial.contains("width: 312px"));
        assertFalse(partial.contains("width: 252px"));
    }

    @Test
    public void formRowsUseResponsiveCssGridAndSectionRhythm() throws IOException {
        String partial = partial("halo");

        assertTrue(partial.contains("display: grid !important;"));
        assertTrue(partial.contains("grid-template-columns: repeat(2, minmax(0, 1fr));"));
        assertTrue(partial.contains("column-gap: 16px;"));
        assertTrue(partial.contains("row-gap: 12px;"));
        assertTrue(partial.contains("position: static !important;"));
        assertTrue(partial.contains("grid-column: 1 / span 2;"));
        assertTrue(partial.contains("@media (max-width: 960px)"));
        assertTrue(partial.contains("grid-template-columns: minmax(0, 1fr);"));
        assertTrue(partial.contains(".iteraction-list-section-card > .v-spacing"));
        assertTrue(partial.contains("height: 12px !important;"));
    }

    @Test
    public void everyFieldCaptionInNewSectionCardsStaysAboveItsOwnControl() throws IOException {
        String partial = partial("halo");

        assertTrue(partial.contains(".iteraction-list-section-card .v-has-caption"));
        assertTrue(partial.contains("flex-direction: column !important;"));
        assertTrue(partial.contains(".iteraction-list-section-card .v-has-caption > .v-caption"));
        assertTrue(partial.contains("position: static !important;"));
        assertTrue(partial.contains("margin: 0 0 5px !important;"));
        assertTrue(partial.contains("text-align: left !important;"));
        assertTrue(partial.contains(
                ".iteraction-list-section-card .v-has-caption > .v-caption .v-captiontext"));
    }

    @Test
    public void activePopularButtonsHaveWhiteBoldCaption() throws IOException {
        String partial = partial("halo");

        assertTrue(partial.contains(".iteraction-list-popular-button:not(.v-disabled):not([disabled])"));
        assertTrue(partial.contains("color: #ffffff !important;"));
        assertTrue(partial.contains("font-weight: 700 !important;"));
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
