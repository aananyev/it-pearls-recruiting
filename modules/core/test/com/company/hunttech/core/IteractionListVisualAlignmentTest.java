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
 * Защищает согласованную геометрию XML и локального SCSS IteractionListEdit:
 * одинаковые OvaFallbackImage, label-навигацию, sidebar-карточки и AUTO-блок результата.
 */
public class IteractionListVisualAlignmentTest {

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
    public void descriptorKeepsRequestedSidebarAndResultGeometry() throws IOException {
        String descriptor = descriptor();

        assertEquals(2, count(descriptor, "width=\"96px\""));
        assertEquals(2, count(descriptor, "height=\"96px\""));
        assertEquals(2, count(descriptor, "ovalWidth=\"96px\""));
        assertEquals(2, count(descriptor, "ovalHeight=\"96px\""));
        assertEquals(4, count(descriptor, "<column width=\"50%\"/>"));
        assertEquals(0, count(descriptor, "<column flex=\"1\"/>"));

        assertTrue(descriptor.contains("id=\"iteractionListNavigationTitle\""));
        assertTrue(descriptor.contains(
                "stylename=\"label-navigation iteraction-list-navigation\""));
        assertTrue(descriptor.contains(
                "stylename=\"label-nav-title iteraction-list-navigation-title\""));
        assertEquals(4, count(descriptor, "label-nav-item iteraction-list-nav-item"));

        assertEquals(2, count(descriptor,
                "stylename=\"edit-form-control iteraction-list-primary-picker\""));
        assertOrdered(descriptor,
                "id=\"iteractionCandidateNameLabel\"",
                "id=\"iteractionVacancyNameLabel\"",
                "id=\"iteractionServiceCard\"",
                "id=\"vacancyStateSummary\"",
                "id=\"iteractionListNavigation\"");

        String result = section(
                descriptor,
                "id=\"resultAccordion\"",
                "id=\"commentAccordion\"");
        assertTrue(result.contains("height=\"AUTO\""));
        assertTrue(result.contains("iteraction-list-result-section"));
        assertTrue(result.contains("id=\"resultAccordionBody\""));
        assertTrue(result.contains("iteraction-list-result-body"));
        assertTrue(result.contains("iteraction-list-result-grid"));

        String state = section(
                descriptor,
                "id=\"vacancyStateSummary\"",
                "id=\"iteractionListNavigation\"");
        assertTrue(state.startsWith("id=\"vacancyStateSummary\""));
        assertEquals(2, count(state, "width=\"50%\""));

        assertTrue(descriptor.contains("id=\"iteractionServiceCard\""));
        assertTrue(descriptor.contains("id=\"iteractionServiceFields\""));
        assertTrue(descriptor.contains("id=\"iteractionVacancyCard\""));
        assertTrue(descriptor.contains("id=\"sidebarVacancyNameLabel\""));
        assertTrue(descriptor.contains("property=\"vacancy.vacansyName\""));
        assertTrue(descriptor.contains("value=\"msg://msgVacancyName\""));
    }

    @Test
    public void everyThemeLoadsFinalVisualAlignmentAfterLegacyLayers() throws IOException {
        String expectedPartial = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/"
                        + "iteraction-list-visual-alignment.scss");

        for (String theme : THEMES) {
            String partial = readProjectFile(
                    "modules/web/themes/" + theme
                            + "/com.company.hunttech/iteraction-list-visual-alignment.scss");
            String styles = readProjectFile(
                    "modules/web/themes/" + theme + "/styles.scss");

            assertEquals("SCSS должен быть идентичен: " + theme, expectedPartial, partial);
            assertTrue(theme, styles.contains(
                    "@import \"com.company.hunttech/iteraction-list-visual-alignment\";"));
            assertTrue(theme, styles.contains(
                    "@include iteraction-list-visual-alignment-theme;"));
            assertOrdered(styles,
                    "@include edit-screen-shared-styles;",
                    "@include iteraction-list-reference-finish-theme;",
                    "@include iteraction-list-visual-alignment-theme;");
        }
    }

    @Test
    public void finalScssContainsAllNineVisualContracts() throws IOException {
        String partial = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/"
                        + "iteraction-list-visual-alignment.scss");

        assertTrue(partial.contains("width: 96px !important"));
        assertTrue(partial.contains("object-view-box: inset(8%)"));
        assertTrue(partial.contains("object-view-box: inset(5%)"));
        assertTrue(partial.contains(".label-navigation"));
        assertTrue(partial.contains(".label-nav-title"));
        assertTrue(partial.contains(".label-nav-item-active"));
        assertTrue(partial.contains("display: flex !important"));
        assertTrue(partial.contains("flex-direction: row !important"));
        assertTrue(partial.contains(".iteraction-list-service-card"));
        assertTrue(partial.contains(".iteraction-list-vacancy-card"));
        assertTrue(partial.contains(".iteraction-list-primary-picker"));
        assertTrue(partial.contains(".iteraction-list-result-section"));
        assertTrue(partial.contains(".iteraction-list-service-card .c-datefield-layout"));
        assertTrue(partial.contains("flex: 0 0 56px;"));
        assertTrue(partial.contains("min-height: 61px !important;"));
        assertTrue(partial.contains(".iteraction-list-subscription-filter > label"));
        assertTrue(partial.contains("padding: 0 0 0 28px !important;"));
        assertTrue(partial.contains(".v-slot-iteraction-list-sidebar"));
        assertTrue(partial.contains("width: 312px !important;"));
        assertTrue(partial.contains(".iteraction-list-form-grid.iteraction-list-participants-grid"));
        assertTrue(partial.contains("height: 59px !important;"));
        assertTrue(partial.contains(".iteraction-list-form-grid.iteraction-list-result-grid"));
        assertTrue(partial.contains("height: 126px !important;"));
        assertTrue(partial.contains("padding: 19px 8px 0 !important;"));
        assertTrue(partial.contains("left: 50% !important;"));
        assertTrue(partial.contains("height: auto !important"));
        assertTrue(partial.contains(".iteraction-list-vacancy-name-value"));
        assertFalse(partial.contains("\n  .v-label {"));
        assertFalse(partial.contains("\n  .v-button {"));
        assertFalse(partial.contains("\n  .v-panel {"));
    }

    private String descriptor() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/"
                        + "iteraction-list-edit.xml");
    }

    private String section(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        assertTrue("Не найден начальный маркер: " + startMarker, start >= 0);
        int end = text.indexOf(endMarker, start);
        assertTrue("Не найден конечный маркер: " + endMarker, end > start);
        return text.substring(start, end);
    }

    private void assertOrdered(String text, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int current = text.indexOf(marker);
            assertTrue("Не найден маркер: " + marker, current >= 0);
            assertTrue("Нарушен порядок: " + marker, current > previous);
            previous = current;
        }
    }

    private int count(String text, String token) {
        int result = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            result++;
            index += token.length();
        }
        return result;
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
