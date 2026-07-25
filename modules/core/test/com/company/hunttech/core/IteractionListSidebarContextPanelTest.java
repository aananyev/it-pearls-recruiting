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
 * Защищает читаемую геометрию контекстной панели IteractionListEdit
 * и одинаковый локальный CSS-контракт во всех поддерживаемых темах.
 */
public class IteractionListSidebarContextPanelTest {

    private static final List<String> THEMES = Arrays.asList(
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark");

    @Test
    public void sidebarUsesReadableWidthAndPreservesLegacyBindings() throws IOException {
        String xml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");

        // Регрессия защищает presentation-reflow, не меняя legacy ID и data binding.
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar\"\n                  width=\"272px\""));
        assertTrue(xml.contains("id=\"numberIteractionField\""));
        assertTrue(xml.contains("property=\"numberIteraction\""));
        assertTrue(xml.contains("id=\"dateIteractionField\""));
        assertTrue(xml.contains("property=\"dateIteraction\""));
        assertTrue(xml.contains("id=\"statusOfVacansyLabel\""));
        assertTrue(xml.contains("expand=\"statusOfVacansyLabel\""));
        assertTrue(xml.contains("id=\"currentPriorityLabel\""));
        assertTrue(xml.contains("expand=\"currentPriorityLabel\""));
        assertTrue(xml.contains("id=\"outstaffingCostHBox\""));
        assertTrue(xml.contains("property=\"vacancy.outstaffingCost\""));
    }

    @Test
    public void contextMetricsUseDedicatedRowsInsteadOfDenseCaptionLayout() throws IOException {
        String xml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");

        assertTrue(xml.contains("stylename=\"iteraction-list-service-field\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar-value-row\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar-cost\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar-cost-line\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar-cost-value\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar-cost-unit\""));
    }

    @Test
    public void allThemesKeepScopedCaptionAndSidebarGeometry() throws IOException {
        for (String theme : THEMES) {
            String editorScss = readProjectFile(
                    "modules/web/themes/" + theme + "/com.company.hunttech/iteraction-list-editor.scss");
            String navigationScss = readProjectFile(
                    "modules/web/themes/" + theme + "/com.company.hunttech/iteraction-list-accordion-navigation.scss");

            assertTrue(editorScss.contains(".iteraction-list-context-card .v-panel-caption"));
            assertFalse(editorScss.contains(
                    ".iteraction-list-context-card .v-panel-caption,\n    .iteraction-list-context-card .v-caption"));
            assertTrue(editorScss.contains(".iteraction-list-service-fields .v-caption"));
            assertTrue(editorScss.contains("width: 272px !important;"));
            assertTrue(editorScss.contains("width: 252px !important;"));
            assertTrue(editorScss.contains(".iteraction-list-sidebar-cost-value"));
            assertTrue(navigationScss.contains("min-width: 272px !important;"));
            assertTrue(navigationScss.contains("min-width: 252px !important;"));
            assertFalse(navigationScss.contains("min-width: 212px !important;"));
        }
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
