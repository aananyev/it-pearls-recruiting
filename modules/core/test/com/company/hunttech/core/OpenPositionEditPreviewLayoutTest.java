package com.company.hunttech.core;

import org.junit.Test;

import javax.xml.parsers.DocumentBuilderFactory;
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
 * Защищает изолированность preview-экрана OpenPositionEdit и подтверждает, что
 * визуальный рефакторинг не изменяет data/loaders/JPQL и legacy-вызовы формы.
 */
public class OpenPositionEditPreviewLayoutTest {

    private static final String PREVIEW_XML =
            "modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit-preview.xml";
    private static final String LEGACY_XML =
            "modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit.xml";
    private static final String PREVIEW_CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEditPreview.java";
    private static final String LEGACY_CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEdit.java";

    private static final String[] TAB_IDS = {
            "tabOpenPosition",
            "laborAgreementTab",
            "tabPayments",
            "tabJobDescription",
            "tabFiles",
            "tabExercise",
            "tabMemoForInterview",
            "tabTemplateLetter",
            "tabSkills",
            "tabOpenPositionNews",
            "tabApproval",
            "commentsTab"
    };

    @Test
    public void previewDescriptorParsesAndLegacyScreenRemainsIndependent() throws Exception {
        DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(projectRoot().resolve(PREVIEW_XML).toFile());

        String legacyController = readProjectFile(LEGACY_CONTROLLER);
        String previewController = readProjectFile(PREVIEW_CONTROLLER);

        assertTrue(legacyController.contains("@UiController(\"hunttech_OpenPosition.edit\")"));
        assertTrue(legacyController.contains("@UiDescriptor(\"open-position-edit.xml\")"));
        assertFalse(legacyController.contains("editPreview"));
        assertTrue(previewController.contains("extends OpenPositionEdit"));
        assertTrue(previewController.contains("@UiController(\"hunttech_OpenPosition.editPreview\")"));
        assertTrue(previewController.contains("@UiDescriptor(\"open-position-edit-preview.xml\")"));
        assertTrue(previewController.contains("@Route(\"open-position-edit-preview\")"));
    }

    @Test
    public void previewUsesExactLegacyDataAndFacetContracts() throws IOException {
        String legacy = readProjectFile(LEGACY_XML);
        String preview = readProjectFile(PREVIEW_XML);

        assertEquals(normalize(section(legacy, "<data>", "</data>")),
                normalize(section(preview, "<data>", "</data>")));
        assertEquals(normalize(section(legacy, "<facets>", "</facets>")),
                normalize(section(preview, "<facets>", "</facets>")));
    }

    @Test
    public void legacyTabIdentifiersRemainAvailableAndPaymentsStayInsideAgreements() throws IOException {
        String descriptor = readProjectFile(PREVIEW_XML);
        int previous = -1;
        for (String tabId : TAB_IDS) {
            String marker = "<tab id=\"" + tabId + "\"";
            int current = descriptor.indexOf(marker);
            assertTrue("Не найдена вкладка " + tabId, current >= 0);
            assertTrue("Нарушен порядок вкладки " + tabId, current > previous);
            previous = current;
        }

        String paymentsTab = section(descriptor,
                "<tab id=\"tabPayments\"",
                "<tab id=\"tabJobDescription\"");
        assertTrue(paymentsTab.contains("visible=\"false\""));
        assertTrue(descriptor.contains("property=\"commandCandidate\""));
        assertTrue(descriptor.contains("id=\"previewNavPayments\""));
        assertTrue(startTag(descriptor, "previewNavPayments").contains("visible=\"false\""));
        assertTrue(startTag(descriptor, "previewNavPayments")
                .contains("open-position-preview-payments-navigation"));

        int agreementsTab = descriptor.indexOf("<tab id=\"laborAgreementTab\"");
        int companyPayments = descriptor.indexOf("id=\"groupBoxPaymentsDetail\"");
        int researcherPayments = descriptor.indexOf("id=\"groupBoxPaymentsResearcher\"");
        int recruiterPayments = descriptor.indexOf("id=\"groupBoxPaymentsRecrutier\"");
        int hiddenPaymentsTab = descriptor.indexOf("<tab id=\"tabPayments\"");
        assertTrue("Оплата компании должна находиться в трудовых договорах",
                companyPayments > agreementsTab && companyPayments < hiddenPaymentsTab);
        assertTrue("Оплата ресерчерам должна следовать за оплатой компании",
                researcherPayments > companyPayments && researcherPayments < hiddenPaymentsTab);
        assertTrue("Оплата рекрутерам должна следовать за оплатой ресерчерам",
                recruiterPayments > researcherPayments && recruiterPayments < hiddenPaymentsTab);
        assertFalse("Техническая вкладка оплат должна оставаться пустой",
                paymentsTab.contains("groupBoxPayments"));
    }

    @Test
    public void sidebarAndWorkspaceFollowApprovedEditLayout() throws IOException {
        String descriptor = readProjectFile(PREVIEW_XML);

        assertOrdered(descriptor,
                "id=\"openPositionPreviewMainLayout\"",
                "id=\"openPositionPreviewSidebar\"",
                "id=\"openPositionPreviewIdentity\"",
                "id=\"labelOpenPosition\"",
                "id=\"openPositionPreviewNavigation\"",
                "id=\"openPositionPreviewSummary\"",
                "id=\"openPositionPreviewWorkspace\"",
                "id=\"openPositionPreviewToolbar\"",
                "id=\"tabSheetOpenPosition\"",
                "id=\"editActions\"");
        assertTrue(descriptor.contains("id=\"openPositionPreviewSidebar\"")
                && descriptor.contains("width=\"264px\""));
        assertTrue(descriptor.contains("id=\"projectLogoImage\" width=\"88px\""));
        assertTrue(descriptor.contains("caption=\"Сохранить и закрыть\""));
        assertTrue(descriptor.contains("caption=\"Отмена\""));
        assertEquals(12, count(descriptor, "open-position-preview-nav-item"));
        assertEquals(12, count(descriptor, "invoke=\"previewOpen"));

        // Бизнес-id, утверждённые актуальным master для OpenPositionEdit,
        // сохраняются в семантически соответствующих блоках preview.
        String[] currentMasterIds = {
                "mainTabScrollBox",
                "vacancyTitleSpacerHBox",
                "laborAgreementButtonsPanel",
                "companyPaymentsVBox",
                "researcherPaymentsVBox",
                "recrutierPaymentsVBox",
                "someFilesButtonsPanel",
                "openPositionNewsButtonsPanel",
                "subscribePositionButton",
                "windowCloseButton"
        };
        for (String componentId : currentMasterIds) {
            assertTrue("Не сохранён business id " + componentId,
                    descriptor.contains("id=\"" + componentId + "\""));
        }
    }

    @Test
    public void largeBlocksAndTablesUseCollapsibleAccordions() throws IOException {
        String descriptor = readProjectFile(PREVIEW_XML);

        String[] accordionIds = {
                "identityStatusAccordion",
                "commandFieldHBox",
                "commandVacancyAccordion",
                "projectLocationAccordion",
                "positionCountAccordion",
                "salaryAccordion",
                "laborAgreementParametersAccordion",
                "laborAgreementTableAccordion",
                "groupBoxPaymentsDetail",
                "workExperienceGroupBox",
                "descriptionTextsAccordion",
                "shortDescriptionAccordion",
                "filesTableAccordion",
                "exerciseAccordion",
                "memoAccordion",
                "templateLetterAccordion",
                "newsTableAccordion",
                "procActionsBox",
                "commentsAccordion"
        };
        for (String accordionId : accordionIds) {
            String element = startTag(descriptor, accordionId);
            assertTrue("Секция должна быть collapsable: " + accordionId,
                    element.contains("collapsable=\"true\""));
        }

        assertNestedInAccordion(descriptor, "laborAgreementDataGrid", "laborAgreementTableAccordion");
        assertNestedInAccordion(descriptor, "someFilesTable", "filesTableAccordion");
        assertNestedInAccordion(descriptor, "openPostionNewsDataGrid", "newsTableAccordion");
        assertNestedInAccordion(descriptor, "commentsScrollBox", "commentsAccordion");

        // Блок «Требуемые Навыки» восстановлен как в legacy: плоская структура
        // (без аккордеон-обёртки) — skillsBox с expand на дерево навыков.
        int skillsBox = descriptor.indexOf("id=\"skillsBox\"");
        int skillsTree = descriptor.indexOf("id=\"openPositionSkillsListTable\"");
        assertTrue("Не найден skillsBox", skillsBox >= 0);
        assertTrue("Дерево навыков должно следовать за skillsBox", skillsTree > skillsBox);
        assertTrue("skillsBox должен растягивать дерево навыков (expand)",
                descriptor.contains("expand=\"openPositionSkillsListTable\""));
        assertTrue("Блок навыков не должен иметь аккордеон-обёртку",
                !descriptor.substring(skillsBox, skillsTree).contains("groupBox"));
    }

    @Test
    public void criticalBindingsActionsAndInvokesRemainAvailable() throws IOException {
        String descriptor = readProjectFile(PREVIEW_XML);

        String[] bindings = {
                "property=\"vacansyID\"",
                "property=\"vacansyName\"",
                "property=\"grade\"",
                "property=\"commandCandidate\"",
                "property=\"projectName\"",
                "property=\"cityPosition\"",
                "property=\"numberPosition\"",
                "property=\"salaryMin\"",
                "property=\"salaryMax\"",
                "property=\"comment\"",
                "property=\"commentEn\"",
                "property=\"exercise\"",
                "property=\"memoForInterview\"",
                "property=\"templateLetter\""
        };
        for (String binding : bindings) {
            assertTrue("Не найден binding " + binding, descriptor.contains(binding));
        }

        String[] actionsAndInvokes = {
                "action=\"windowCommitAndClose\"",
                "action=\"windowClose\"",
                "invoke=\"subscribePosition\"",
                "invoke=\"generateNameFieldButton\"",
                "invoke=\"addListCity\"",
                "invoke=\"setSalaryFieldButtonInvoke\"",
                "invoke=\"addShortDescription\"",
                "invoke=\"rescanJobDescription\"",
                "invoke=\"addOpenPositionNewsButton\""
        };
        for (String action : actionsAndInvokes) {
            assertTrue("Не найден action/invoke " + action, descriptor.contains(action));
        }
    }

    @Test
    public void previewProvidesAllInheritedUiComponentsAndListenerTargets() throws IOException {
        String controller = readProjectFile(LEGACY_CONTROLLER);
        String descriptor = readProjectFile(PREVIEW_XML);

        // Все UI-компоненты, которые базовый контроллер получает через @Inject,
        // обязаны присутствовать в параллельном XML; сервисы и data API сюда не входят.
        java.util.regex.Pattern injectedUi = java.util.regex.Pattern.compile(
                "@Inject\\s+private\\s+(?:Label<[^>]+>|DateField<[^>]+>|Timer|" +
                        "LookupPickerField<[^>]+>|TextField<[^>]+>|CheckBox|RichTextArea|" +
                        "LookupField<[^>]+>|RadioButtonGroup(?:<[^>]+>)?|GroupBoxLayout|" +
                        "TreeDataGrid<[^>]+>|DataGrid<[^>]+>|ScrollBoxLayout|" +
                        "OvaFallbackImage|TabSheet|ProcActionsFragment)\\s+(\\w+)\\s*;");
        java.util.regex.Matcher injectedMatcher = injectedUi.matcher(controller);
        int injectedCount = 0;
        while (injectedMatcher.find()) {
            String componentId = injectedMatcher.group(1);
            assertTrue("Preview не содержит inherited UI component: " + componentId,
                    descriptor.contains("id=\"" + componentId + "\""));
            injectedCount++;
        }
        assertTrue("Не найдены inherited UI injections", injectedCount > 40);

        // @Named пути проверяются по каждому segment id: TabSheet.Tab и Accordion.Tab.
        java.util.regex.Matcher namedMatcher = java.util.regex.Pattern.compile(
                "@Named\\(\"([^\"]+)\"\\)").matcher(controller);
        while (namedMatcher.find()) {
            for (String componentId : namedMatcher.group(1).split("\\.")) {
                assertTrue("Preview не содержит @Named component: " + componentId,
                        descriptor.contains("id=\"" + componentId + "\""));
            }
        }

        // Любой inherited listener/install должен иметь реальную цель с прежним id.
        java.util.regex.Matcher subscribeMatcher = java.util.regex.Pattern.compile(
                "@Subscribe\\(\"([^\"]+)\"\\)").matcher(controller);
        while (subscribeMatcher.find()) {
            String componentId = subscribeMatcher.group(1);
            assertTrue("Preview не содержит @Subscribe target: " + componentId,
                    descriptor.contains("id=\"" + componentId + "\""));
        }

        java.util.regex.Matcher installMatcher = java.util.regex.Pattern.compile(
                "@Install\\(to\\s*=\\s*\"([^\"]+)\"").matcher(controller);
        while (installMatcher.find()) {
            String componentId = installMatcher.group(1).split("\\.")[0];
            assertTrue("Preview не содержит @Install target: " + componentId,
                    descriptor.contains("id=\"" + componentId + "\""));
        }
    }

    @Test
    public void previewIsNotRegisteredInMenuOrBrowseScreens() throws IOException {
        String menu = readProjectFile("modules/web/src/com/company/hunttech/web-menu.xml");
        String browse = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/openposition/open-position-browse.xml");

        assertFalse(menu.contains("hunttech_OpenPosition.editPreview"));
        assertFalse(menu.contains("open-position-edit-preview"));
        assertFalse(browse.contains("hunttech_OpenPosition.editPreview"));
        assertFalse(browse.contains("open-position-edit-preview"));
    }

    private void assertNestedInAccordion(String descriptor,
                                          String componentId,
                                          String accordionId) {
        int accordionStart = descriptor.indexOf("id=\"" + accordionId + "\"");
        int component = descriptor.indexOf("id=\"" + componentId + "\"", accordionStart);
        int accordionEnd = descriptor.indexOf("</groupBox>", component);
        assertTrue("Не найден accordion " + accordionId, accordionStart >= 0);
        assertTrue("Компонент не находится после accordion " + componentId, component > accordionStart);
        assertTrue("Не найден конец accordion для " + componentId, accordionEnd > component);
    }

    private String startTag(String text, String id) {
        int marker = text.indexOf("id=\"" + id + "\"");
        assertTrue("Не найден компонент " + id, marker >= 0);
        int start = text.lastIndexOf('<', marker);
        int end = text.indexOf('>', marker);
        assertTrue(start >= 0 && end > start);
        return text.substring(start, end + 1);
    }

    private String section(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        assertTrue("Не найден начальный маркер " + startMarker, start >= 0);
        int end = text.indexOf(endMarker, start);
        assertTrue("Не найден конечный маркер " + endMarker, end > start);
        return text.substring(start, end + endMarker.length());
    }

    private String normalize(String value) {
        return value.replaceAll("<!--(?s:.*?)-->", "")
                .replaceAll("\\s+", "");
    }

    private void assertOrdered(String text, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int current = text.indexOf(marker);
            assertTrue("Не найден обязательный маркер " + marker, current >= 0);
            assertTrue("Нарушен порядок маркера " + marker, current > previous);
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
