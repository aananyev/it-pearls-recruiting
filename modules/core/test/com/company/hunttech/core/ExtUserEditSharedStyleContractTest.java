package com.company.hunttech.core;

import org.junit.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
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
 * Защищает визуальную миграцию ExtUserEdit на общий edit-* / label-* API
 * без изменения legacy CUBA-контрактов пользователя, ролей и замещений.
 */
public class ExtUserEditSharedStyleContractTest {

    private static final String DESCRIPTOR =
            "modules/web/src/com/company/hunttech/web/screens/extuser/ext-user-edit.xml";
    private static final String CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/extuser/ExtUserEditor.java";

    @Test
    public void descriptorUsesSharedEditScreenRoles() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        List<String> requiredStyles = Arrays.asList(
                "ext-user-editor",
                "edit-screen-layout",
                "edit-sidebar",
                "edit-sidebar-visual",
                "edit-sidebar-identity",
                "edit-sidebar-title",
                "edit-sidebar-subtitle",
                "edit-sidebar-summary",
                "edit-sidebar-hint",
                "edit-sidebar-spacer",
                "label-navigation",
                "label-nav-title",
                "label-nav-item",
                "label-nav-item-active",
                "edit-workspace",
                "edit-toolbar",
                "edit-toolbar-title",
                "edit-toolbar-description",
                "edit-toolbar-actions",
                "edit-tabs",
                "edit-card",
                "edit-card-title",
                "edit-help",
                "edit-form-control",
                "edit-accordion-section",
                "edit-footer-actions"
        );

        for (String style : requiredStyles) {
            assertTrue("Не найден общий stylename: " + style,
                    descriptor.contains(style));
        }

        assertTrue(descriptor.contains("id=\"profilePanel\""));
        assertTrue(descriptor.contains("width=\"270px\""));
        assertTrue(descriptor.contains("id=\"generalUserNavigation\""));
        assertTrue(descriptor.contains("id=\"emailUserNavigation\""));
        assertTrue(descriptor.contains("id=\"aiUserNavigation\""));
    }

    @Test
    public void navigationIsPresentationOnlyAndKeepsBaseItemStyle() throws IOException {
        String controller = readProjectFile(CONTROLLER);

        assertTrue(controller.contains("addSelectedTabChangeListener"));
        assertTrue(controller.contains("removeStyleName(ACTIVE_NAV_STYLE)"));
        assertTrue(controller.contains("addStyleName(ACTIVE_NAV_STYLE)"));
        assertTrue(controller.contains("section.setExpanded(true)"));
        assertTrue(controller.contains("target.focus()"));
        assertTrue(controller.contains("fieldComponent.addStyleName(\"edit-form-control\")"));

        assertFalse(controller.contains("setSelectedTab("));
        assertFalse(controller.contains("dataManager"));
        assertFalse(controller.contains("userManagementService.changePassword"));
        assertFalse(controller.contains("passwordEncryption"));
        assertFalse(controller.contains("dataManager.commit"));
        assertFalse(controller.contains("rolesDs"));
        assertFalse(controller.contains("substitutionsDs"));
        assertFalse(controller.contains("setValue("));
    }

    @Test
    public void existingBindingsActionsAndIdentifiersRemainIntact() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        List<String> protectedContracts = Arrays.asList(
                "class=\"com.company.hunttech.web.screens.extuser.ExtUserEditor\"",
                "datasource=\"userDs\"",
                "view=\"extUser-view\"",
                "property=\"userRoles\"",
                "property=\"substitutions\"",
                "select e from hunttech_UserAiConfiguration e where e.user = :ds$userDs",
                "id=\"fieldGroupLeft\"",
                "id=\"fieldGroupRight\"",
                "id=\"contactsFieldGroup\"",
                "id=\"rolesTable\"",
                "id=\"substTable\"",
                "id=\"emailFieldGroupLeft\"",
                "id=\"emailFieldGroupRight\"",
                "id=\"emailFieldPasswordRequired\"",
                "id=\"emailFieldGroupUser\"",
                "id=\"emailFieldGroupPasswords\"",
                "id=\"aiConfigsTable\"",
                "id=\"changePasswordBtn\"",
                "invoke=\"changePassword\"",
                "action=\"rolesTable.edit\"",
                "action=\"rolesTable.remove\"",
                "action=\"substTable.add\"",
                "action=\"substTable.edit\"",
                "action=\"substTable.remove\"",
                "screen=\"editWindowActions\""
        );

        for (String contract : protectedContracts) {
            assertTrue("Нарушен защищённый XML-контракт: " + contract,
                    descriptor.contains(contract));
        }
    }

    @Test
    public void emailSettingsUseFiveFullWidthAccordions() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        assertAccordion(descriptor, "propertiesEmailBox", "false");
        assertAccordion(descriptor, "emailPortsBox", "true");
        assertAccordion(descriptor, "emailAuthenticationBox", "true");
        assertAccordion(descriptor, "emailAccountsBox", "true");
        assertAccordion(descriptor, "emailPasswordsBox", "true");
        assertFalse(descriptor.contains("id=\"propertiesEmailGrid\""));
    }

    @Test
    public void localScssIsIdenticalAndConnectedAfterSharedApiInAllThemes() throws IOException {
        List<String> themes = Arrays.asList(
                "halo",
                "havana",
                "helium",
                "hover",
                "hunttech-modern",
                "hunttech-modern-light",
                "hunttech-modern-dark"
        );

        String referenceScss = null;
        for (String theme : themes) {
            String scss = readProjectFile(
                    "modules/web/themes/" + theme
                            + "/com.company.hunttech/ext-user-editor.scss");
            if (referenceScss == null) {
                referenceScss = scss;
            } else {
                assertEquals("Локальный ExtUserEdit SCSS различается между темами",
                        referenceScss, scss);
            }

            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            int sharedImport = styles.indexOf(
                    "@import \"com.company.hunttech/edit-screen-shared-styles\";");
            int screenImport = styles.indexOf(
                    "@import \"com.company.hunttech/ext-user-editor\";");
            int sharedInclude = styles.indexOf("@include edit-screen-shared-styles;");
            int screenInclude = styles.indexOf("@include ext-user-editor-theme;");

            assertTrue(sharedImport >= 0 && screenImport > sharedImport);
            assertTrue(sharedInclude >= 0 && screenInclude > sharedInclude);
        }

        assertNotNull(referenceScss);
        assertTrue(referenceScss.contains(".ext-user-editor"));
        assertTrue(referenceScss.contains("#172638"));
        assertTrue(referenceScss.contains("#ffb11b"));
        assertFalse(referenceScss.contains("\n  .v-button"));
        assertFalse(referenceScss.contains("\n  .v-label"));
        assertFalse(referenceScss.contains("\n  .v-table"));
    }

    @Test
    public void descriptorIsValidXmlAndEveryOpeningElementIsDocumented() throws Exception {
        Path descriptorPath = projectRoot().resolve(DESCRIPTOR);
        try (InputStream inputStream = Files.newInputStream(descriptorPath)) {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        }

        List<String> lines = Files.readAllLines(descriptorPath, StandardCharsets.UTF_8);
        for (int index = 0; index < lines.size(); index++) {
            String trimmed = lines.get(index).trim();
            if (!isOpeningElement(trimmed)) {
                continue;
            }

            int previous = index - 1;
            while (previous >= 0 && lines.get(previous).trim().isEmpty()) {
                previous--;
            }
            assertTrue("Перед opening element на строке " + (index + 1)
                            + " отсутствует смысловой XML-комментарий: " + trimmed,
                    previous >= 0 && lines.get(previous).trim().startsWith("<!--"));
        }
    }

    private void assertAccordion(String descriptor, String id, String collapsed) {
        int start = descriptor.indexOf("id=\"" + id + "\"");
        assertTrue("Не найдена accordion-секция " + id, start >= 0);
        String fragment = descriptor.substring(start, Math.min(descriptor.length(), start + 600));
        assertTrue(fragment.contains("width=\"100%\""));
        assertTrue(fragment.contains("collapsable=\"true\""));
        assertTrue(fragment.contains("collapsed=\"" + collapsed + "\""));
        assertTrue(fragment.contains("showAsPanel=\"true\""));
        assertTrue(fragment.contains("edit-accordion-section"));
    }

    private boolean isOpeningElement(String line) {
        return line.startsWith("<")
                && !line.startsWith("</")
                && !line.startsWith("<?")
                && !line.startsWith("<!--")
                && !line.startsWith("<![CDATA[")
                && !line.startsWith("<!DOCTYPE");
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
