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
 * Контрактный тест визуального редизайна OpenPositionEdit (legacy-форма).
 *
 * Фиксирует presentation-слой редизайна: общий edit-* / label-* UI API,
 * локальный namespace .open-position-editor, идентичность семи SCSS-копий,
 * порядок подключения в семи темах и неизменность Java-контроллера.
 * Бизнес-логика вакансии тестом не проверяется.
 */
public class OpenPositionEditLayoutContractTest {

    private static final String EDIT_XML =
            "modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit.xml";
    private static final String EDIT_CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEdit.java";
    private static final String LOCAL_STYLE = "open-position-editor.scss";

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
    public void descriptorParsesAndUsesSharedEditRoles() throws Exception {
        DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(projectRoot().resolve(EDIT_XML).toFile());

        String descriptor = readProjectFile(EDIT_XML);

        List<String> descriptorRoles = Arrays.asList(
                "edit-screen-layout",
                "open-position-editor",
                "edit-sidebar",
                "edit-sidebar-visual",
                "edit-sidebar-identity",
                "edit-sidebar-title",
                "edit-sidebar-subtitle",
                "edit-sidebar-summary",
                "edit-sidebar-warning",
                "edit-sidebar-spacer",
                "label-navigation",
                "label-nav-title",
                "label-nav-item",
                "label-nav-item-active",
                "edit-workspace",
                "edit-toolbar",
                "edit-toolbar-title",
                "edit-toolbar-description",
                "edit-tabs",
                "edit-form-control",
                "edit-accordion-section",
                "edit-footer-actions"
        );
        for (String role : descriptorRoles) {
            assertTrue("В XML отсутствует общий stylename " + role,
                    descriptor.contains(role));
        }

        assertTrue("Root не двухпанельный: sidebar должен предшествовать workspace",
                descriptor.indexOf("id=\"openPositionSidebar\"")
                        < descriptor.indexOf("id=\"openPositionWorkspace\""));
        assertTrue("Размер dialogMode не соответствует решению арбитра 9-1",
                descriptor.contains("height=\"900px\"") && descriptor.contains("width=\"1400px\""));
    }

    @Test
    public void typicalFieldsReceiveDirectEditFormControl() throws IOException {
        String descriptor = readProjectFile(EDIT_XML);

        String[] fieldIds = {
                "vacansyIDTextField",
                "vacansyNameField",
                "gradeLookupPickerField",
                "closingDateDateField",
                "priorityField",
                "commentPriority",
                "parentOpenPositionField",
                "positionTypeField",
                "remoteWorkField",
                "remoteWorkCommentField",
                "projectNameField",
                "companyDepartamentField",
                "companyNameField",
                "cityOpenPositionField",
                "numberPositionField",
                "openPositionFieldSalaryMin",
                "openPositionFieldSalaryMax",
                "openPositionFieldSalaryIE",
                "salaryCommentTextFiels",
                "registrationForWorkField",
                "outstaffingCostTextField",
                "textFieldPercentOrSum",
                "textFieldCompanyPayment",
                "textFieldResearcherSalaryPercentOrSum",
                "textFieldResearcherSalary",
                "textFieldRecrutierPercentOrSum",
                "textFieldRecrutierSalary",
                "openPositionRichTextArea",
                "openPositionEnRichTextArea",
                "openPositionStandartDescriptionRichTextArea",
                "openPositionWhoIsThisGuyRichTextArea",
                "shortDescriptionTextArea",
                "exerciseRichTextArea",
                "memoForInterviewRichTextArea",
                "templateLetterRichTextArea"
        };
        for (String fieldId : fieldIds) {
            String tag = startTag(descriptor, fieldId);
            assertTrue("Поле " + fieldId + " не имеет edit-form-control",
                    tag.contains("edit-form-control"));
        }
    }

    @Test
    public void groupBoxSectionsUseAccordionContractAndKeepCollapsable() throws IOException {
        String descriptor = readProjectFile(EDIT_XML);

        String[] sectionIds = {
                "openPositionEditorIdentifiersCard",
                "commandFieldHBox",
                "commandOrVacancyGroupBox",
                "projectTypeGroupBox",
                "personnelCountGroupBox",
                "salaryGroupBox",
                "laborAgreementGroupBox",
                "groupBoxPaymentsDetail",
                "groupBoxPaymentsResearcher",
                "groupBoxPaymentsRecrutier",
                "workExperienceGroupBox",
                "procActionsBox"
        };
        for (String sectionId : sectionIds) {
            String tag = startTag(descriptor, sectionId);
            assertTrue("Секция " + sectionId + " не имеет edit-accordion-section",
                    tag.contains("edit-accordion-section"));
            assertTrue("Секция " + sectionId + " не отображается как панель (showAsPanel)",
                    tag.contains("showAsPanel=\"true\""));
        }

        // collapsable/collapsed legacy-контракта сохранены
        // (procActionsBox legacy-контракт не имел collapsable — не проверяется).
        String[] collapsableIds = {
                "commandFieldHBox",
                "commandOrVacancyGroupBox",
                "projectTypeGroupBox",
                "personnelCountGroupBox",
                "salaryGroupBox",
                "groupBoxPaymentsDetail",
                "workExperienceGroupBox"
        };
        for (String sectionId : collapsableIds) {
            assertTrue("Секция " + sectionId + " потеряла collapsable",
                    startTag(descriptor, sectionId).contains("collapsable=\"true\""));
        }
        assertTrue("Секция деталей оплаты должна оставаться свёрнутой",
                startTag(descriptor, "groupBoxPaymentsDetail").contains("collapsed=\"true\""));
        assertTrue("Секция опыта должна оставаться свёрнутой",
                startTag(descriptor, "workExperienceGroupBox").contains("collapsed=\"true\""));
    }

    @Test
    public void paymentsMovedInsideAgreementsAndTechnicalTabStaysHidden() throws IOException {
        String descriptor = readProjectFile(EDIT_XML);

        int agreementsTab = descriptor.indexOf("<tab id=\"laborAgreementTab\"");
        int companyPayments = descriptor.indexOf("id=\"groupBoxPaymentsDetail\"");
        int researcherPayments = descriptor.indexOf("id=\"groupBoxPaymentsResearcher\"");
        int recruiterPayments = descriptor.indexOf("id=\"groupBoxPaymentsRecrutier\"");
        int hiddenPaymentsTab = descriptor.indexOf("<tab id=\"tabPayments\"");

        assertTrue("Платёжные секции не внутри трудовых соглашений",
                companyPayments > agreementsTab && companyPayments < hiddenPaymentsTab);
        assertTrue("Оплата ресерчерам должна следовать за оплатой компании",
                researcherPayments > companyPayments && researcherPayments < hiddenPaymentsTab);
        assertTrue("Оплата рекрутерам должна следовать за оплатой ресерчерам",
                recruiterPayments > researcherPayments && recruiterPayments < hiddenPaymentsTab);

        assertTrue("Техническая вкладка tabPayments должна оставаться скрытой",
                startTag(descriptor, "tabPayments").contains("visible=\"false\""));
        assertTrue("Техническая вкладка tabPayments должна быть пустой",
                !section(descriptor, "<tab id=\"tabPayments\"", "<tab id=\"tabJobDescription\"")
                        .contains("groupBoxPayments"));
    }

    @Test
    public void localScssIsIdenticalConnectedAndNamespacedInAllThemes() throws IOException {
        String referenceLocalScss = null;

        for (String theme : THEMES) {
            String themeRoot = "modules/web/themes/" + theme + "/";
            String localScss = readProjectFile(themeRoot + "com.company.hunttech/" + LOCAL_STYLE);

            if (referenceLocalScss == null) {
                referenceLocalScss = localScss;
            } else {
                assertEquals("Локальный SCSS различается между темами: " + theme,
                        referenceLocalScss, localScss);
            }

            String styles = readProjectFile(themeRoot + "styles.scss");
            int sharedImport = styles.indexOf(
                    "@import \"com.company.hunttech/edit-screen-shared-styles\";");
            int localImport = styles.indexOf(
                    "@import \"com.company.hunttech/open-position-editor\";");
            int sharedInclude = styles.indexOf("@include edit-screen-shared-styles;");
            int localInclude = styles.indexOf("@include open-position-editor-theme;");

            assertTrue("Нет или нарушен порядок import в теме " + theme,
                    sharedImport >= 0 && localImport > sharedImport);
            assertTrue("Нет или нарушен порядок include в теме " + theme,
                    sharedInclude >= 0 && localInclude > sharedInclude);
        }

        assertNotNull(referenceLocalScss);
        assertTrue(referenceLocalScss.contains("@mixin open-position-editor-theme"));
        assertTrue(referenceLocalScss.contains(".open-position-editor {"));
        assertTrue(referenceLocalScss.contains("#172638"));
        assertTrue(referenceLocalScss.contains("#ffb11b"));
        assertTrue(referenceLocalScss.contains(".label-nav-item-active"));
        assertTrue(referenceLocalScss.contains("rgba(255, 255, 255, 0.08)"));
        assertTrue(referenceLocalScss.contains("rgba(255, 177, 27, 0.12)"));
        assertTrue(referenceLocalScss.contains(".open-position-editor-table-variant5"));
        assertTrue(referenceLocalScss.contains(".open-position-editor-richtext-variant5"));
        assertTrue(referenceLocalScss.contains(".open-position-editor-footer-actions"));
        assertFalse("В локальном SCSS не должно быть вложенных @media (CUBA Sass)",
                referenceLocalScss.contains("@media"));
    }

    @Test
    public void localScssHasNoUnscopedGlobalVaadinSelectors() throws IOException {
        String scss = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/" + LOCAL_STYLE);

        List<String> forbidden = Arrays.asList(
                "\n  .v-label {",
                "\n  .v-button {",
                "\n  .v-table {",
                "\n  .v-panel {",
                "\n  .v-textfield {",
                "\n  .v-tabsheet {"
        );
        for (String selector : forbidden) {
            assertFalse("Найден неограниченный глобальный селектор " + selector.trim(),
                    scss.contains(selector));
        }
        assertTrue("Namespace .open-position-editor должен присутствовать",
                scss.contains(".open-position-editor"));
    }

    @Test
    public void legacyControllerAndNamespaceIsolationRemainIntact() throws IOException {
        String descriptor = readProjectFile(EDIT_XML);
        String controller = readProjectFile(EDIT_CONTROLLER);
        String scss = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/" + LOCAL_STYLE);

        // Java-контроллер не изменён: в нём нет presentation-классов редизайна.
        assertFalse(controller.contains("open-position-editor"));
        assertFalse(controller.contains("edit-footer-actions"));
        assertFalse(controller.contains("edit-accordion-section"));

        // Изоляция namespace: legacy-форма не использует preview-классы другой формы.
        assertFalse(descriptor.contains("open-position-preview-"));
        assertFalse(scss.contains("open-position-preview-"));
        assertFalse(scss.contains("job-candidate-"));
    }

    private String section(String text, String start, String end) {
        int s = text.indexOf(start);
        assertTrue("Не найден маркер " + start, s >= 0);
        int e = text.indexOf(end, s);
        assertTrue("Не найден маркер " + end, e >= 0);
        return text.substring(s, e);
    }

    private String startTag(String descriptor, String componentId) {
        int start = descriptor.indexOf("id=\"" + componentId + "\"");
        assertTrue("Не найден компонент " + componentId, start >= 0);
        int tagStart = descriptor.lastIndexOf('<', start);
        int tagEnd = descriptor.indexOf('>', start);
        assertTrue("Компонент " + componentId + " не имеет закрывающего '>'", tagEnd >= 0);
        return descriptor.substring(tagStart, tagEnd);
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
