package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Закрепляет применение общего визуального контракта Edit-форм к персональному
 * dashboard без изменения persistent model, виджетов и бизнес-данных.
 */
public class MainScreenDashboardSharedStyleContractTest {

    private static final String[] THEMES = {
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark"
    };

    @Test
    public void dashboardUsesSharedWorkspaceAndLocalRootNamespace() throws IOException {
        String descriptor = source(
                "modules/web/src/com/company/hunttech/web/screens/mainscreen/ext-main-screen.xml");

        assertTrue(descriptor.contains("code=\"recruiting-dashboard\""));
        assertTrue(descriptor.contains("timerDelay=\"60\""));
        assertTrue(descriptor.contains(
                "stylename=\"edit-workspace recruiter-dashboard-root\""));
        assertTrue(descriptor.contains("width=\"100%\""));
        assertTrue(descriptor.contains("height=\"100%\""));

        // Presentation-задача не должна подменять persistent dashboard другой формой.
        assertFalse(descriptor.contains("jsonPath="));
        assertFalse(descriptor.contains("class="));
    }

    @Test
    public void allSevenThemesUseIdenticalDashboardPartial() throws IOException {
        String canonicalPartial = null;

        for (String theme : THEMES) {
            String partialPath = "modules/web/themes/" + theme
                    + "/com.company.hunttech/recruiter-dashboard-shared-styles.scss";
            String partial = source(partialPath);
            String styles = source("modules/web/themes/" + theme + "/styles.scss");

            assertTrue(partial.contains("@mixin recruiter-dashboard-shared-styles"));
            assertTrue(partial.contains(".recruiter-dashboard-root"));
            assertTrue(partial.contains(
                    ".recruiter-dashboard-root .widget-border"));
            assertTrue(partial.contains(
                    ".recruiter-dashboard-root .widget-table-header"));
            assertTrue(partial.contains("border-radius: 8px"));
            assertTrue(partial.contains("min-height: 38px"));
            assertTrue(partial.contains("@media (max-width: 1366px)"));
            assertTrue(partial.contains("#ffb11b"));

            assertTrue(styles.contains(
                    "@import \"com.company.hunttech/recruiter-dashboard-shared-styles\";"));
            assertTrue(styles.contains(
                    "@include recruiter-dashboard-shared-styles;"));

            if (canonicalPartial == null) {
                canonicalPartial = partial;
            } else {
                assertEquals("SCSS dashboard должен быть идентичен во всех темах",
                        canonicalPartial, partial);
            }
        }
    }

    @Test
    public void dashboardStylesReuseEditContractGeometryWithoutGlobalVaadinOverrides()
            throws IOException {
        String dashboard = source(
                "modules/web/themes/halo/com.company.hunttech/"
                        + "recruiter-dashboard-shared-styles.scss");
        String editContract = source(
                "modules/web/themes/halo/com.company.hunttech/"
                        + "edit-screen-shared-styles.scss");

        assertTrue(editContract.contains(".edit-workspace"));
        assertTrue(editContract.contains(".edit-card"));
        assertTrue(editContract.contains("border-radius: 8px"));
        assertTrue(editContract.contains("0 2px 8px rgba(15, 23, 42, 0.05)"));
        assertTrue(editContract.contains("min-height: 38px"));

        assertTrue(dashboard.contains("padding: 16px 20px"));
        assertTrue(dashboard.contains("border-radius: 8px"));
        assertTrue(dashboard.contains("0 2px 8px rgba(15, 23, 42, 0.05)"));
        assertTrue(dashboard.contains("min-height: 38px"));
        assertTrue(dashboard.contains("overflow-x: hidden"));

        // Любые Vaadin-селекторы допустимы только после локального root namespace.
        assertFalse(dashboard.contains("\n  .v-label {"));
        assertFalse(dashboard.contains("\n  .v-button {"));
        assertFalse(dashboard.contains("\n  .v-table {"));
        assertFalse(dashboard.contains("\n  .v-panel {"));
    }

    @Test
    public void mainScreenDocumentationDescribesTheVisualContract() throws IOException {
        String specification = source("docs/ui/HrmMainScreen_Spec.md");

        assertTrue(specification.contains("## Визуальный контракт персонального dashboard"));
        assertTrue(specification.contains("recruiter-dashboard-root"));
        assertTrue(specification.contains("всех семи тем"));
        assertTrue(specification.contains("persistent model"));
    }

    private String source(String relativePath) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8);
    }

    private Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Не найден корень проекта HRM HuntTech");
    }
}
