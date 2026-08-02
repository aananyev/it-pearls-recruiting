package com.company.hunttech.core;

import org.junit.Test;

import javax.xml.parsers.DocumentBuilderFactory;
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
 * Защищает применение общего edit-* / label-* UI API к OpenPositionEditPreview.
 * Тест фиксирует presentation-контракт и не проверяет бизнес-логику вакансии.
 */
public class OpenPositionEditPreviewSharedStyleContractTest {

    private static final String PREVIEW_XML =
            "modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit-preview.xml";
    private static final String PREVIEW_CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEditPreview.java";
    private static final String LEGACY_CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEdit.java";
    private static final String SHARED_STYLE = "edit-screen-shared-styles.scss";
    private static final String LOCAL_STYLE = "open-position-preview.scss";

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
    public void descriptorAndControllerUseSharedEditRoles() throws Exception {
        DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(projectRoot().resolve(PREVIEW_XML).toFile());

        String descriptor = readProjectFile(PREVIEW_XML);
        String controller = readProjectFile(PREVIEW_CONTROLLER);

        List<String> descriptorRoles = Arrays.asList(
                "edit-screen-layout",
                "edit-sidebar",
                "edit-sidebar-visual",
                "edit-sidebar-identity",
                "edit-sidebar-title",
                "edit-sidebar-subtitle",
                "edit-sidebar-summary",
                "edit-sidebar-spacer",
                "label-navigation",
                "label-nav-title",
                "label-nav-item",
                "label-nav-item-active",
                "edit-workspace",
                "edit-toolbar",
                "edit-toolbar-title",
                "edit-toolbar-description",
                "edit-tabs"
        );
        for (String role : descriptorRoles) {
            assertTrue("В XML отсутствует общий stylename " + role,
                    descriptor.contains(role));
        }

        List<String> runtimeRoles = Arrays.asList(
                "edit-sidebar-warning",
                "edit-sidebar-hint",
                "edit-help",
                "edit-form-control",
                "edit-footer-actions",
                "edit-accordion-section",
                "edit-workspace-scroll",
                "edit-workspace-content"
        );
        for (String role : runtimeRoles) {
            assertTrue("Контроллер не назначает общий stylename " + role,
                    controller.contains(role));
        }

        assertTrue(controller.contains("openPositionPreviewSidebar.setWidth(\"270px\")"));
        assertTrue(controller.contains("section.setShowAsPanel(true)"));
        assertTrue(controller.contains("section.removeStyleName(\"light\")"));
        assertTrue(controller.contains("section.removeStyleName(\"edit-card\")"));
        assertTrue(controller.contains("applySharedWorkspaceStyles(openPositionPreviewWorkspace)"));
    }

    @Test
    public void typicalFieldsReceiveDirectSharedControlStyle() throws IOException {
        String controller = readProjectFile(PREVIEW_CONTROLLER);

        assertTrue(controller.contains("if (isSharedFormControl(component))"));
        assertTrue(controller.contains("component.addStyleName(FORM_CONTROL_STYLE)"));
        assertTrue(controller.contains("component instanceof TextField"));
        assertTrue(controller.contains("component instanceof TextArea"));
        assertTrue(controller.contains("component instanceof LookupField"));
        assertTrue(controller.contains("component instanceof LookupPickerField"));
        assertTrue(controller.contains("component instanceof SuggestionPickerField"));
        assertTrue(controller.contains("component instanceof DateField"));
        assertTrue(controller.contains("component instanceof RichTextArea"));
        assertFalse(controller.contains("component instanceof CheckBox"));
        assertFalse(controller.contains("component instanceof RadioButtonGroup"));
    }

    @Test
    public void navigationKeepsBaseStyleAndChangesOnlyActiveState() throws IOException {
        String controller = readProjectFile(PREVIEW_CONTROLLER);

        assertTrue(controller.contains("button.addStyleName(BASE_NAV_STYLE)"));
        assertTrue(controller.contains("button.removeStyleName(ACTIVE_NAV_STYLE)"));
        assertTrue(controller.contains("button.addStyleName(ACTIVE_NAV_STYLE)"));
        assertFalse(controller.contains("setStyleName(active ?"));
        assertFalse(controller.contains("NAV_ACTIVE_STYLE"));
    }

    @Test
    public void sharedAndLocalScssAreIdenticalAndConnectedInAllThemes() throws IOException {
        String referenceSharedScss = null;
        String referenceLocalScss = null;

        for (String theme : THEMES) {
            String themeRoot = "modules/web/themes/" + theme + "/";
            String sharedScss = readProjectFile(
                    themeRoot + "com.company.hunttech/" + SHARED_STYLE);
            String localScss = readProjectFile(
                    themeRoot + "com.company.hunttech/" + LOCAL_STYLE);

            if (referenceSharedScss == null) {
                referenceSharedScss = sharedScss;
                referenceLocalScss = localScss;
            } else {
                assertEquals("Shared SCSS различается между темами: " + theme,
                        referenceSharedScss, sharedScss);
                assertEquals("Локальный preview SCSS различается между темами: " + theme,
                        referenceLocalScss, localScss);
            }

            String styles = readProjectFile(themeRoot + "styles.scss");
            int sharedImport = styles.indexOf(
                    "@import \"com.company.hunttech/edit-screen-shared-styles\";");
            int localImport = styles.indexOf(
                    "@import \"com.company.hunttech/open-position-preview\";");
            int sharedInclude = styles.indexOf("@include edit-screen-shared-styles;");
            int localInclude = styles.indexOf("@include open-position-preview-theme;");

            assertTrue("Нет или нарушен порядок import в теме " + theme,
                    sharedImport >= 0 && localImport > sharedImport);
            assertTrue("Нет или нарушен порядок include в теме " + theme,
                    sharedInclude >= 0 && localInclude > sharedInclude);
        }

        assertNotNull(referenceSharedScss);
        assertNotNull(referenceLocalScss);
        assertTrue(referenceSharedScss.contains(".edit-sidebar"));
        assertTrue(referenceSharedScss.contains(".edit-accordion-section"));
        assertTrue(referenceSharedScss.contains(".edit-form-control"));
        assertTrue(referenceSharedScss.contains(".edit-footer-actions"));
        assertTrue(referenceSharedScss.contains("@media (max-width: 1366px)"));

        assertTrue(referenceLocalScss.contains(".open-position-preview"));
        assertTrue(referenceLocalScss.contains("#172638"));
        assertTrue(referenceLocalScss.contains("#ffb11b"));
        assertTrue(referenceLocalScss.contains(".label-nav-item-active"));
        assertTrue(referenceLocalScss.contains(".edit-accordion-section .v-panel-content"));
        assertFalse(referenceLocalScss.contains("\n  .v-label {"));
        assertFalse(referenceLocalScss.contains("\n  .v-button {"));
        assertFalse(referenceLocalScss.contains("\n  .v-table {"));
    }

    @Test
    public void presentationLayerDoesNotModifyLegacyController() throws IOException {
        String previewController = readProjectFile(PREVIEW_CONTROLLER);
        String legacyController = readProjectFile(LEGACY_CONTROLLER);

        assertTrue(previewController.contains("applySharedEditScreenContract"));
        assertFalse(legacyController.contains("applySharedEditScreenContract"));
        assertFalse(legacyController.contains("edit-footer-actions"));
        assertFalse(legacyController.contains("edit-accordion-section"));
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
