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
 * Фиксирует presentation-коррекцию sidebar OpenPositionEditPreview.
 *
 * <p>Тест проверяет только локальный SCSS-контракт: резерв высоты логотипа,
 * отделение title и компактную сетку summary. Java/XML, data binding и
 * бизнес-логика экрана не должны зависеть от corrective layer.</p>
 */
public class OpenPositionEditPreviewSidebarUsabilityContractTest {

    private static final String LOCAL_STYLE =
            "com.company.hunttech/open-position-preview-sidebar-usability.scss";
    private static final String IMPORT =
            "@import \"com.company.hunttech/open-position-preview-sidebar-usability\";";
    private static final String INCLUDE =
            "@include open-position-preview-sidebar-usability-theme;";

    private static final List<String> THEMES = Arrays.asList(
            "halo",
            "havana",
            "helium",
            "hover",
            "hunttech-modern",
            "hunttech-modern-light",
            "hunttech-modern-dark"
    );

    @Test
    public void correctiveScssIsIdenticalAndConnectedAfterPreviewLayer() throws IOException {
        String reference = null;

        for (String theme : THEMES) {
            String themeRoot = "modules/web/themes/" + theme + "/";
            String corrective = readProjectFile(themeRoot + LOCAL_STYLE);
            String styles = readProjectFile(themeRoot + "styles.scss");

            if (reference == null) {
                reference = corrective;
            } else {
                assertEquals("Corrective SCSS различается между темами: " + theme,
                        reference, corrective);
            }

            int previewImport = styles.indexOf(
                    "@import \"com.company.hunttech/open-position-preview\";");
            int correctiveImport = styles.indexOf(IMPORT);
            int previewInclude = styles.indexOf("@include open-position-preview-theme;");
            int correctiveInclude = styles.indexOf(INCLUDE);

            assertTrue("Corrective import отсутствует или стоит не после preview: " + theme,
                    previewImport >= 0 && correctiveImport > previewImport);
            assertTrue("Corrective include отсутствует или стоит не после preview: " + theme,
                    previewInclude >= 0 && correctiveInclude > previewInclude);
        }

        assertNotNull(reference);
        assertTrue(reference.contains("@mixin open-position-preview-sidebar-usability-theme"));
        assertTrue(reference.contains(".open-position-preview-logo-box"));
        assertTrue(reference.contains("height: 96px !important"));
        assertTrue(reference.contains("width: 88px !important"));
        assertTrue(reference.contains("-webkit-line-clamp: 2"));
        assertTrue(reference.contains("grid-template-columns: 66px minmax(0, 1fr)"));
        assertTrue(reference.contains(".label-nav-title,"));
        assertTrue(reference.contains("font-size: 11px !important"));
        assertTrue(reference.contains(".label-nav-item,"));
        assertTrue(reference.contains("min-height: 32px !important"));
        assertFalse(reference.contains("@media"));
        assertFalse(reference.contains("min-height: 21px !important"));
    }

    @Test
    public void correctiveLayerRemainsScopedToPreview() throws IOException {
        String reference = readProjectFile(
                "modules/web/themes/halo/" + LOCAL_STYLE);

        assertTrue(reference.contains(".open-position-preview {"));
        assertFalse(reference.contains("\n  .v-label {"));
        assertFalse(reference.contains("\n  .v-button {"));
        assertFalse(reference.contains("\n  .v-gridlayout {"));
        assertFalse(reference.contains("\n  .v-panel {"));
    }

    @Test
    public void javaAndDescriptorsDoNotOwnCorrectivePresentationContract() throws IOException {
        String previewController = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEditPreview.java");
        String previewDescriptor = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit-preview.xml");
        String legacyController = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEdit.java");
        String legacyDescriptor = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit.xml");

        String marker = "open-position-preview-sidebar-usability";
        assertFalse(previewController.contains(marker));
        assertFalse(previewDescriptor.contains(marker));
        assertFalse(legacyController.contains(marker));
        assertFalse(legacyDescriptor.contains(marker));

        assertTrue(previewDescriptor.contains("id=\"openPositionPreviewLogoBox\""));
        assertTrue(previewDescriptor.contains("id=\"openPositionPreviewSummary\""));
        assertTrue(previewDescriptor.contains("id=\"summaryVacansyIDLabel\""));
        assertTrue(previewDescriptor.contains("id=\"summaryRegistrationForWorkLabel\""));
    }

    private String readProjectFile(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8
        );
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
