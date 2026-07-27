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
 * Защищает постоянную контекстную sidebar и единый shared SCSS source,
 * используемый семью темами через относительные symbolic links.
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
        assertTrue(xml.contains("id=\"outstaffingCostHBox\""));
        assertTrue(xml.contains("property=\"vacancy.outstaffingCost\""));
    }

    @Test
    public void candidatePhotoIsPrimaryAndProjectLogoIsSecondary() throws IOException {
        String xml = descriptor();
        String identity = section(
                xml,
                "stylename=\"iteraction-list-identity-images edit-sidebar-visual\"",
                "id=\"iteractionCandidateNameLabel\"");

        assertTrue(identity.contains("id=\"candidateImage\""));
        assertTrue(identity.contains("width=\"112px\""));
        assertTrue(identity.contains("ovalWidth=\"112px\""));
        assertTrue(identity.contains("id=\"projectLogoImage\""));
        assertTrue(identity.contains("width=\"80px\""));
        assertTrue(identity.contains("ovalWidth=\"80px\""));
        assertTrue(identity.contains("stylename=\"iteraction-list-project-image\""));
        assertFalse(identity.contains(
                "id=\"projectLogoImage\"\n                                          width=\"112px\""));
    }

    @Test
    public void oneCanonicalPartialFeedsAllSevenThemesThroughSymlinks() throws IOException {
        Path root = projectRoot();
        Path canonical = root.resolve(
                "modules/web/themes/common/edit-screen-shared-styles.scss");

        assertTrue(Files.isRegularFile(canonical));
        String shared = new String(Files.readAllBytes(canonical), StandardCharsets.UTF_8);
        assertTrue(shared.contains("@mixin edit-screen-shared-styles"));
        assertTrue(shared.contains(".label-navigation"));
        assertTrue(shared.contains(".edit-workspace"));
        assertFalse(shared.contains("\n  .v-label {"));
        assertFalse(shared.contains("\n  .v-button {"));

        for (String theme : THEMES) {
            Path link = root.resolve(
                    "modules/web/themes/" + theme
                            + "/com.company.hunttech/edit-screen-shared-styles.scss");
            assertTrue(theme + ": shared partial должен быть symbolic link",
                    Files.isSymbolicLink(link));
            assertEquals(
                    "../../common/edit-screen-shared-styles.scss",
                    Files.readSymbolicLink(link).toString());

            String styles = readProjectFile(
                    "modules/web/themes/" + theme + "/styles.scss");
            assertTrue(theme + ": отсутствует import shared partial",
                    styles.contains(
                            "@import \"com.company.hunttech/edit-screen-shared-styles\";"));
            assertTrue(theme + ": отсутствует shared mixin",
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
