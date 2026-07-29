package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает постоянную sidebar IteractionListEdit и порядок критичного контекста вакансии.
 */
public class IteractionListSidebarContextPanelTest {

    private static final List<String> THEMES = Arrays.asList(
            "halo",
            "havana",
            "helium",
            "hover",
            "hunttech-modern",
            "hunttech-modern-light",
            "hunttech-modern-dark");

    @Test
    public void sidebarPreservesEntityIdentityContextAndLegacyBindings() throws IOException {
        String xml = descriptor();

        assertTrue(xml.contains("id=\"candidateImage\""));
        assertTrue(xml.contains("property=\"candidate.fileImageFace\""));
        assertTrue(xml.contains("fallbackThemePath=\"icons/no-programmer.jpeg\""));
        assertTrue(xml.contains("id=\"projectLogoImage\""));
        assertTrue(xml.contains("fallbackThemePath=\"icons/no-company.png\""));
        assertTrue(xml.contains("id=\"iteractionCandidateNameLabel\""));
        assertTrue(xml.contains("property=\"candidate.fullName\""));
        assertTrue(xml.contains("id=\"iteractionVacancyNameLabel\""));
        assertTrue(xml.contains("property=\"vacancy.vacansyName\""));
        assertTrue(xml.contains("id=\"numberIteractionField\""));
        assertTrue(xml.contains("property=\"numberIteraction\""));
        assertTrue(xml.contains("id=\"dateIteractionField\""));
        assertTrue(xml.contains("property=\"dateIteraction\""));
        assertOrdered(xml,
                "id=\"iteractionCandidateNameLabel\"",
                "id=\"iteractionVacancyNameLabel\"",
                "id=\"iteractionServiceCard\"",
                "id=\"vacancyStateSummary\"");
        assertTrue(xml.contains("id=\"outstaffingCostHBox\""));
        assertTrue(xml.contains("property=\"vacancy.outstaffingCost\""));
    }

    @Test
    public void vacancyStatusAndPriorityAppearBeforeLabelNavigation() throws IOException {
        String xml = descriptor();

        assertOrdered(xml,
                "id=\"iteractionVacancyNameLabel\"",
                "id=\"vacancyStateSummary\"",
                "id=\"statusOfVacansyLabel\"",
                "id=\"currentPriorityLabel\"",
                "id=\"iteractionListNavigation\"");

        String stateSummary = section(
                xml,
                "id=\"vacancyStateSummary\"",
                "id=\"iteractionListNavigation\"");
        assertTrue(stateSummary.contains("id=\"alternativeVacancyLinkButton\""));
        assertTrue(stateSummary.contains("id=\"trafficLighterImage\""));

        String lowerVacancyCard = section(
                xml,
                "iteraction-list-vacancy-card",
                "id=\"iteractionListSidebarSpacer\"");
        assertFalse(lowerVacancyCard.contains("id=\"statusOfVacansyLabel\""));
        assertFalse(lowerVacancyCard.contains("id=\"currentPriorityLabel\""));
    }

    @Test
    public void candidatePhotoIsPrimaryAndProjectLogoIsSecondary() throws IOException {
        String xml = descriptor();
        String identity = section(
                xml,
                "stylename=\"iteraction-list-identity-images edit-sidebar-visual\"",
                "id=\"iteractionCandidateNameLabel\"");

        assertTrue(identity.contains("id=\"candidateImage\""));
        assertTrue(identity.contains("width=\"96px\""));
        assertTrue(identity.contains("ovalWidth=\"96px\""));
        assertTrue(identity.contains("id=\"projectLogoImage\""));
        assertEquals(2, count(identity, "width=\"96px\""));
        assertEquals(2, count(identity, "ovalWidth=\"96px\""));
        assertTrue(identity.contains("iteraction-list-project-image"));

        String alignment = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/"
                        + "iteraction-list-visual-alignment.scss");
        String projectImage = section(
                alignment,
                ".iteraction-list-project-image,",
                ".iteraction-list-identity-images");
        assertTrue(projectImage.contains("width: 96px !important"));
        assertTrue(projectImage.contains("height: 96px !important"));
    }

    @Test
    public void allSevenThemesContainRealSharedStyleCopies() throws IOException {
        Path root = projectRoot();

        assertFalse(Files.exists(root.resolve(
                "modules/web/themes/common/edit-screen-shared-styles.scss")));

        for (String theme : THEMES) {
            Path partial = root.resolve(
                    "modules/web/themes/" + theme
                            + "/com.company.hunttech/edit-screen-shared-styles.scss");
            assertTrue(theme + ": отсутствует theme-local shared partial",
                    Files.isRegularFile(partial));
            assertFalse(theme + ": symbolic link больше не допускается",
                    Files.isSymbolicLink(partial));

            String shared = new String(Files.readAllBytes(partial), StandardCharsets.UTF_8);
            assertTrue(theme + ": отсутствует shared mixin",
                    shared.contains("@mixin edit-screen-shared-styles"));
            assertTrue(theme + ": отсутствует label-navigation",
                    shared.contains(".label-navigation"));
            assertTrue(theme + ": отсутствует edit-workspace",
                    shared.contains(".edit-workspace"));

            String styles = readProjectFile(
                    "modules/web/themes/" + theme + "/styles.scss");
            assertTrue(theme + ": отсутствует import shared partial",
                    styles.contains("@import \"com.company.hunttech/edit-screen-shared-styles\";"));
            assertTrue(theme + ": отсутствует shared mixin include",
                    styles.contains("@include edit-screen-shared-styles;"));
        }
    }

    @Test
    public void sidebarUsesSharedRolesDirectlyInXml() throws IOException {
        String xml = descriptor();

        assertTrue(xml.contains("edit-sidebar"));
        assertTrue(xml.contains("edit-sidebar-visual"));
        assertTrue(xml.contains("edit-sidebar-identity"));
        assertTrue(xml.contains("edit-sidebar-title"));
        assertTrue(xml.contains("edit-sidebar-subtitle"));
        assertTrue(xml.contains("edit-sidebar-summary"));
        assertTrue(xml.contains("edit-sidebar-warning"));
        assertTrue(xml.contains("edit-sidebar-spacer"));
    }

    private String descriptor() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
    }

    private String section(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        assertTrue("Не найден начальный XML-маркер: " + startMarker, start >= 0);
        int end = text.indexOf(endMarker, start);
        assertTrue("Не найден конечный XML-маркер: " + endMarker, end > start);
        return text.substring(start, end);
    }

    private void assertOrdered(String text, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int current = text.indexOf(marker);
            assertTrue("Не найден обязательный маркер: " + marker, current >= 0);
            assertTrue("Нарушен порядок маркера: " + marker, current > previous);
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
