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
 * Защищает семантический порядок и общий визуальный контракт sidebar
 * IteractionListEdit без изменения legacy component ID и bindings.
 */
public class IteractionListSidebarContextPanelTest {

    private static final List<String> THEMES = Arrays.asList(
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark");

    @Test
    public void sidebarPreservesEntityIdentityAndLegacyBindings() throws IOException {
        String xml = descriptor();

        assertTrue(xml.contains("id=\"iteractionCandidateNameLabel\""));
        assertTrue(xml.contains("property=\"candidate.fullName\""));
        assertTrue(xml.contains("id=\"iteractionVacancyNameLabel\""));
        assertTrue(xml.contains("property=\"vacancy.vacansyName\""));
        assertTrue(xml.contains("id=\"candidateImage\""));
        assertTrue(xml.contains("property=\"candidate.fileImageFace\""));
        assertTrue(xml.contains("id=\"projectLogoImage\""));
        assertTrue(xml.contains("fallbackThemePath=\"icons/no-company.png\""));
        assertTrue(xml.contains("id=\"numberIteractionField\""));
        assertTrue(xml.contains("property=\"numberIteraction\""));
        assertTrue(xml.contains("id=\"dateIteractionField\""));
        assertTrue(xml.contains("property=\"dateIteraction\""));
        assertTrue(xml.contains("id=\"outstaffingCostHBox\""));
        assertTrue(xml.contains("property=\"vacancy.outstaffingCost\""));
    }

    @Test
    public void presentationAdapterAddsSharedSidebarRoles() throws IOException {
        String extension = extension();

        assertTrue(extension.contains("\"iteraction-list-sidebar\", \"edit-sidebar\""));
        assertTrue(extension.contains("\"iteraction-list-identity-images\", \"edit-sidebar-visual\""));
        assertTrue(extension.contains("\"iteraction-list-profile-header\", \"edit-sidebar-identity\""));
        assertTrue(extension.contains("\"iteraction-list-profile-title\", \"edit-sidebar-title\""));
        assertTrue(extension.contains("\"iteraction-list-profile-subtitle\", \"edit-sidebar-subtitle\""));
        assertTrue(extension.contains("\"iteraction-list-sidebar-card\", \"edit-sidebar-summary\""));
        assertTrue(extension.contains("\"iteraction-list-sidebar-warning\", \"edit-sidebar-warning\""));
        assertTrue(extension.contains("\"iteraction-list-sidebar-spacer\", \"edit-sidebar-spacer\""));
    }

    @Test
    public void projectLogoRemainsInformativeButVisuallySecondary() throws IOException {
        String extension = extension();

        assertTrue(extension.contains("projectLogoImage.removeStyleName(\"iteraction-list-candidate-image\")"));
        assertTrue(extension.contains("projectLogoImage.addStyleName(\"iteraction-list-project-image\")"));
        assertTrue(extension.contains("projectLogoImage.setWidth(\"80px\")"));
        assertTrue(extension.contains("projectLogoImage.setHeight(\"80px\")"));
    }

    @Test
    public void oneSharedPartialControlsAllSevenThemes() throws IOException {
        String shared = readProjectFile(
                "modules/web/themes/common/edit-screen-shared-styles.scss");

        assertTrue(shared.contains("width: 270px !important"));
        assertTrue(shared.contains("@media (max-width: 1366px)"));
        assertTrue(shared.contains("width: 250px !important"));
        assertTrue(shared.contains(".edit-sidebar"));
        assertTrue(shared.contains(".label-navigation"));
        assertFalse(shared.contains(".v-label {"));
        assertFalse(shared.contains(".v-button {"));

        for (String theme : THEMES) {
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            assertTrue(theme + ": отсутствует import shared partial",
                    styles.contains("../common/edit-screen-shared-styles"));
            assertTrue(theme + ": отсутствует shared mixin",
                    styles.contains("@include edit-screen-shared-styles;"));
        }
    }

    private String descriptor() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
    }

    private String extension() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/"
                        + "IteractionListEditAccordionNavigation.java");
    }

    private String readProjectFile(String relativePath) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relativePath)),
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
