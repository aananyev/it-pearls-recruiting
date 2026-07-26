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
 * Защищает семантический порядок и профильную геометрию sidebar IteractionListEdit.
 */
public class IteractionListSidebarContextPanelTest {

    private static final List<String> THEMES = Arrays.asList(
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark");

    @Test
    public void sidebarShowsEntityIdentityAndPreservesLegacyBindings() throws IOException {
        String xml = descriptor();

        // Новые подписи читают уже загруженные свойства текущего IteractionList.
        assertTrue(xml.contains("id=\"iteractionCandidateNameLabel\""));
        assertTrue(xml.contains("property=\"candidate.fullName\""));
        assertTrue(xml.contains("id=\"iteractionVacancyNameLabel\""));
        assertTrue(xml.contains("property=\"vacancy.vacansyName\""));

        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar\""));
        assertTrue(xml.contains("width=\"296px\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-profile-header\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar-card iteraction-list-service-card\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar-card iteraction-list-vacancy-card\""));
        assertTrue(xml.contains("id=\"candidateImage\""));
        assertTrue(xml.contains("property=\"candidate.fileImageFace\""));
        assertTrue(xml.contains("<ovaFallbackImage id=\"projectLogoImage\""));
        assertTrue(xml.contains("fallbackThemePath=\"icons/no-company.png\""));

        assertCandidateGeometry(componentBlock(xml, "candidateImage"));
        assertProjectGeometry(componentBlock(xml, "projectLogoImage"));

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
    public void contextMetricsUseCardsAndDedicatedInformationRows() throws IOException {
        String xml = descriptor();

        assertTrue(xml.contains("stylename=\"iteraction-list-service-field\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar-info-row\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar-value-row\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar-cost\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar-cost-line\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar-cost-value\""));
        assertTrue(xml.contains("stylename=\"iteraction-list-sidebar-cost-unit\""));
        assertTrue(xml.contains("id=\"iteractionListNavigation\""));
        assertTrue(xml.contains("id=\"iteractionListSidebarSpacer\""));
    }

    @Test
    public void allThemesKeepScopedReferenceGeometry() throws IOException {
        for (String theme : THEMES) {
            String scss = readProjectFile(
                    "modules/web/themes/" + theme
                            + "/com.company.hunttech/iteraction-list-accordion-navigation.scss");
            String finish = readProjectFile(
                    "modules/web/themes/" + theme
                            + "/com.company.hunttech/iteraction-list-reference-finish.scss");

            assertTrue(scss.contains(".iteraction-list-editor"));
            assertTrue(scss.contains("width: 296px !important;"));
            assertTrue(scss.contains("width: 276px !important;"));
            assertTrue(scss.contains("width: 260px !important;"));
            assertTrue(scss.contains(".iteraction-list-profile-header"));
            assertTrue(scss.contains(".iteraction-list-sidebar-card"));
            assertTrue(scss.contains(".iteraction-list-sidebar-card-title"));
            assertTrue(scss.contains(".iteraction-list-service-fields .v-caption"));
            assertTrue(scss.contains(".iteraction-list-sidebar-info-row"));
            assertTrue(scss.contains(".iteraction-list-sidebar-cost-value"));
            assertTrue(scss.contains(".iteraction-list-navigation"));
            assertTrue(finish.contains(".iteraction-list-project-image"));
            assertTrue(finish.contains("object-fit: contain !important;"));
            assertFalse(scss.contains("min-width: 212px !important;"));
            assertFalse(scss.contains(".iteraction-list-context-card .v-panel-caption,"));
        }
    }

    private void assertCandidateGeometry(String imageBlock) {
        assertTrue(imageBlock.contains("width=\"112px\""));
        assertTrue(imageBlock.contains("height=\"112px\""));
        assertTrue(imageBlock.contains("ovalWidth=\"112px\""));
        assertTrue(imageBlock.contains("ovalHeight=\"112px\""));
        assertTrue(imageBlock.contains("stylename=\"iteraction-list-candidate-image\""));
    }

    private void assertProjectGeometry(String imageBlock) {
        assertTrue(imageBlock.contains("width=\"80px\""));
        assertTrue(imageBlock.contains("height=\"80px\""));
        assertTrue(imageBlock.contains("ovalWidth=\"80px\""));
        assertTrue(imageBlock.contains("ovalHeight=\"80px\""));
        assertTrue(imageBlock.contains("stylename=\"iteraction-list-project-image\""));
    }

    private String componentBlock(String xml, String componentId) {
        int start = xml.indexOf("id=\"" + componentId + "\"");
        assertTrue("Не найден компонент " + componentId, start >= 0);
        int end = xml.indexOf("/>", start);
        assertTrue("Не найдено завершение компонента " + componentId, end >= 0);
        return xml.substring(start, end + 2);
    }

    private String descriptor() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
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
