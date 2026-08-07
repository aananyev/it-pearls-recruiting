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
                "openPositionNameGroupBox",
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
    public void nameSectionHoldsSingleRowWithFixedWidths() throws IOException {
        String descriptor = readProjectFile(EDIT_XML);

        // Блок «Наименование» — самая верхняя секция вкладки «Вакансия» (правой части экрана).
        int nameGroup = descriptor.indexOf("id=\"openPositionNameGroupBox\"");
        assertTrue("Блок «Наименование» не найден", nameGroup >= 0);
        int cardsRow = descriptor.indexOf("id=\"openPositionEditorCardsRow1\"");
        assertTrue("Ряд карточек не найден", cardsRow >= 0);
        assertTrue("Блок «Наименование» должен предшествовать ряду карточек вкладки",
                nameGroup < cardsRow);

        // Одна строка: ID + Грейд + Вакансия + кнопка «Генерировать». ID, Грейд и кнопка —
        // фиксированная одинаковая ширина 110px (как поле ID), Вакансия растягивается
        // на свободное место через box.expandRatio; expand на hbox не используется.
        String row = section(descriptor,
                "id=\"openPositionNameRow\"", "</hbox>");
        assertTrue("Строка наименования не должна полагаться на expand (растяжение через expandRatio)",
                !row.contains("expand=\"vacansyNameField\""));

        int idField = row.indexOf("id=\"vacansyIDTextField\"");
        int gradeField = row.indexOf("id=\"gradeLookupPickerField\"");
        int nameField = row.indexOf("id=\"vacansyNameField\"");
        int generateButton = row.indexOf("id=\"generateVacancyNameFieldButton\"");
        assertTrue("Строка не содержит ID/Грейд/Вакансия/Генерировать",
                idField >= 0 && gradeField >= 0 && nameField >= 0 && generateButton >= 0);
        assertTrue("Порядок в строке нарушен (ID → Грейд → Вакансия → Генерировать)",
                idField < gradeField && gradeField < nameField && nameField < generateButton);

        String idTag = startTag(descriptor, "vacansyIDTextField");
        assertTrue("ID должен иметь фиксированную ширину 110px", idTag.contains("width=\"110px\""));
        assertTrue("ID не должен иметь box.expandRatio (фиксированная ширина)",
                !idTag.contains("box.expandRatio"));
        String gradeTag = startTag(descriptor, "gradeLookupPickerField");
        assertTrue("Грейд должен иметь ту же ширину 110px, что и ID",
                gradeTag.contains("width=\"110px\""));
        assertTrue("Грейд не должен иметь box.expandRatio (фиксированная ширина)",
                !gradeTag.contains("box.expandRatio"));
        assertTrue("Грейд должен сохранить optionsContainer", gradeTag.contains("optionsContainer=\"gradeDc\""));
        String nameTag = startTag(descriptor, "vacansyNameField");
        assertTrue("Вакансия должна иметь width=100%", nameTag.contains("width=\"100%\""));
        assertTrue("Вакансия должна растягиваться через box.expandRatio=1",
                nameTag.contains("box.expandRatio=\"1\""));
        assertTrue("Вакансия обязательна", nameTag.contains("required=\"true\""));
        String buttonTag = startTag(descriptor, "generateVacancyNameFieldButton");
        assertTrue("Кнопка «Генерировать» должна иметь ту же ширину 110px, что и ID",
                buttonTag.contains("width=\"110px\""));
        assertTrue("Кнопка «Генерировать» не должна иметь box.expandRatio (фиксированная ширина)",
                !buttonTag.contains("box.expandRatio"));
        assertTrue("Кнопка «Генерировать» должна сохранить invoke", buttonTag.contains("invoke=\"generateNameFieldButton\""));

        // Поля не остались в старых контейнерах карточки «Вакансия».
        assertTrue("ID не должен оставаться в vacancyTopRow",
                !section(descriptor, "id=\"vacancyTopRow\"", "</hbox>")
                        .contains("id=\"vacansyIDTextField\""));
        assertTrue("Грейд не должен оставаться в vacancyTopRow",
                !section(descriptor, "id=\"vacancyTopRow\"", "</hbox>")
                        .contains("id=\"gradeLookupPickerField\""));
        assertTrue("Строка vacancyNameHBox должна быть удалена",
                !descriptor.contains("id=\"vacancyNameHBox\""));
    }

    @Test
    public void sidebarNavigationListsNameParamsVacancyTeamSalaryInOrder() throws IOException {
        String descriptor = readProjectFile(EDIT_XML);

        // Навигационный блок вкладки «Вакансия» содержит ровно 5 пунктов в порядке следования
        // секций формы: «Наименование», «Параметры вакансии», «Вакансия», «Команда», «Зарплатное предложение».
        int navStart = descriptor.indexOf("stylename=\"label-navigation\"");
        assertTrue("Блок label-navigation не найден", navStart >= 0);
        int navEnd = descriptor.indexOf("</vbox>", navStart);
        String nav = descriptor.substring(navStart, navEnd);

        String[] navIds = {
                "openPositionEditorNavName",
                "openPositionEditorNavParams",
                "openPositionEditorNavVacancy",
                "openPositionEditorNavTeam",
                "openPositionEditorNavSalary"
        };
        int prev = -1;
        for (String navId : navIds) {
            int pos = nav.indexOf("id=\"" + navId + "\"");
            assertTrue("Пункт навигации " + navId + " не найден", pos >= 0);
            assertTrue("Пункт навигации " + navId + " не в порядке секций", pos > prev);
            prev = pos;
        }

        // Подписи пунктов соответствуют блокам вкладки.
        assertTrue("«Наименование» должен использовать mainMsg://msgName",
                nav.contains("id=\"openPositionEditorNavName\"" +
                        "\n                    caption=\"mainMsg://msgName\""));
        assertTrue("«Параметры вакансии» должен использовать msg://msgVacancyParams",
                nav.contains("id=\"openPositionEditorNavParams\"" +
                        "\n                    caption=\"msg://msgVacancyParams\""));
        assertTrue("«Вакансия» должен использовать mainMsg://openPositionEditorNavVacancy",
                nav.contains("id=\"openPositionEditorNavVacancy\"" +
                        "\n                    caption=\"mainMsg://openPositionEditorNavVacancy\""));
        assertTrue("«Команда» должен использовать msg://msgCommand",
                nav.contains("id=\"openPositionEditorNavTeam\"" +
                        "\n                    caption=\"msg://msgCommand\""));
        assertTrue("«Зарплатное предложение» должен использовать msg://msgSalaryProposal",
                nav.contains("id=\"openPositionEditorNavSalary\"" +
                        "\n                    caption=\"msg://msgSalaryProposal\""));

        // Устаревшие пункты «Параметры вакансии» (старый id) и «Количество персонала» удалены.
        assertTrue("Пункт «Параметры вакансии» не должен использовать старый id openPositionEditorNavProject",
                !descriptor.contains("openPositionEditorNavProject"));
        assertTrue("Пункт «Количество персонала» должен быть удалён",
                !descriptor.contains("openPositionEditorNavPersonnel"));
    }

    @Test
    public void paramsGroupBoxHoldsPriorityRemoteClosingDateAndComment() throws IOException {
        String descriptor = readProjectFile(EDIT_XML);

        // Новый блок «Параметры вакансии» — отдельная секция после блока «Наименование»
        // и до ряда карточек, со всеми 4 полями (Приоритет/Удаленка/Дата закрытия + Комментарий).
        int paramsGroup = descriptor.indexOf("id=\"openPositionParamsGroupBox\"");
        assertTrue("Блок «Параметры вакансии» не найден", paramsGroup >= 0);
        String paramsTag = startTag(descriptor, "openPositionParamsGroupBox");
        assertTrue("Блок «Параметры вакансии» должен иметь caption msgVacancyParams",
                paramsTag.contains("caption=\"msg://msgVacancyParams\""));
        assertTrue("Блок «Параметры вакансии» должен использовать edit-accordion-section",
                paramsTag.contains("edit-accordion-section"));
        assertTrue("Блок «Параметры вакансии» должен отображаться как панель",
                paramsTag.contains("showAsPanel=\"true\""));

        // Блок расположен между «Наименованием» и рядом карточек.
        int nameGroup = descriptor.indexOf("id=\"openPositionNameGroupBox\"");
        int cardsRow = descriptor.indexOf("id=\"openPositionEditorCardsRow1\"");
        assertTrue("Блок «Параметры вакансии» должен следовать за «Наименованием»",
                nameGroup >= 0 && paramsGroup > nameGroup);
        assertTrue("Блок «Параметры вакансии» должен предшествовать ряду карточек",
                paramsGroup < cardsRow);

        // Строка 1: Приоритет + Удаленка + Дата закрытия (vacancyTopRow).
        String topRow = section(descriptor,
                "id=\"vacancyTopRow\"", "</hbox>");
        assertTrue("Строка vacancyTopRow не содержит приоритет/удалёнку/дату закрытия",
                topRow.contains("id=\"priorityField\"")
                        && topRow.contains("id=\"remoteWorkField\"")
                        && topRow.contains("id=\"closingDateDateField\""));
        assertTrue("Приоритет должен остаться обязательным",
                startTag(descriptor, "priorityField").contains("required=\"true\""));
        assertTrue("Удалёнка должна остаться обязательной",
                startTag(descriptor, "remoteWorkField").contains("required=\"true\""));

        // Строка 2: Комментарий к приоритету (priorityFieldsHBox).
        String commentRow = section(descriptor,
                "id=\"priorityFieldsHBox\"", "</hbox>");
        assertTrue("Строка priorityFieldsHBox не содержит commentPriority",
                commentRow.contains("id=\"commentPriority\""));
        assertTrue("Комментарий должен остаться в dataContainer openPositionDc",
                startTag(descriptor, "commentPriority")
                        .contains("dataContainer=\"openPositionDc\""));

        // Поля перенесены из карточки «Вакансия»: там их быть не должно.
        String card = section(descriptor,
                "id=\"openPositionEditorIdentifiersCard\"", "</groupBox>");
        assertTrue("Приоритет не должен оставаться в карточке «Вакансия»",
                !card.contains("id=\"priorityField\""));
        assertTrue("Удалёнка не должна оставаться в карточке «Вакансия»",
                !card.contains("id=\"remoteWorkField\""));
        assertTrue("Дата закрытия не должна оставаться в карточке «Вакансия»",
                !card.contains("id=\"closingDateDateField\""));
        assertTrue("Комментарий не должен оставаться в карточке «Вакансия»",
                !card.contains("id=\"commentPriority\""));
    }

    @Test
    public void projectFilterCheckBoxesAreHidden() throws IOException {
        String descriptor = readProjectFile(EDIT_XML);

        // Фильтры списка проектов скрыты (visible="false") — по требованию пользователя.
        String onlyOpenTag = startTag(descriptor, "onlyOpenProjectCheckBox");
        assertTrue("Чекбокс «Только открытые проекты» должен быть скрыт",
                onlyOpenTag.contains("visible=\"false\""));
        String withOpenTag = startTag(descriptor, "withOpenPositionCheckBox");
        assertTrue("Чекбокс «Только с открытыми вакансиями» должен быть скрыт",
                withOpenTag.contains("visible=\"false\""));
        // Java-подписки на смену значений должны сохраниться (логика фильтрации не удаляется).
        assertTrue("Чекбокс «Только открытые проекты» потерял caption",
                onlyOpenTag.contains("caption=\"msg://msgOnlyOpenedProject\""));
        assertTrue("Чекбокс «Только с открытыми вакансиями» потерял caption",
                withOpenTag.contains("caption=\"msg://msgWithOpenPosition\""));
    }

    @Test
    public void sidebarImagesMatchIteractionListEditSizeAndAlignment() throws IOException {
        String descriptor = readProjectFile(EDIT_XML);

        // Оба OvaFallbackImage в sidebar — одинакового размера 96×96 (oval 96),
        // как в эталоне IteractionListEdit (candidateImage/projectLogoImage).
        String logoTag = startTag(descriptor, "projectLogoImage");
        assertTrue("Логотип должен иметь width=96px", logoTag.contains("width=\"96px\""));
        assertTrue("Логотип должен иметь height=96px", logoTag.contains("height=\"96px\""));
        assertTrue("Логотип должен иметь ovalWidth=96px", logoTag.contains("ovalWidth=\"96px\""));
        assertTrue("Логотип должен иметь ovalHeight=96px", logoTag.contains("ovalHeight=\"96px\""));
        String ownerTag = startTag(descriptor, "projectOwnerImage");
        assertTrue("Аватар должен иметь width=96px", ownerTag.contains("width=\"96px\""));
        assertTrue("Аватар должен иметь height=96px", ownerTag.contains("height=\"96px\""));
        assertTrue("Аватар должен иметь ovalWidth=96px", ownerTag.contains("ovalWidth=\"96px\""));
        assertTrue("Аватар должен иметь ovalHeight=96px", ownerTag.contains("ovalHeight=\"96px\""));

        // Расположение — рядом друг с другом в одном hbox по центру (эталон IteractionListEdit).
        String box = section(descriptor, "id=\"openPositionEditorLogoBox\"", "</hbox>");
        assertTrue("Изображения должны быть в одном hbox",
                box.contains("id=\"projectLogoImage\"") && box.contains("id=\"projectOwnerImage\""));
        assertTrue("hbox должен центрировать изображения",
                box.contains("align=\"MIDDLE_CENTER\""));
        assertTrue("hbox должен иметь spacing между изображениями",
                box.contains("spacing=\"true\""));
        assertTrue("hbox не должен фиксировать высоту (AUTO)",
                box.contains("height=\"AUTO\""));
        assertTrue("Аватар не должен быть смещён в угол",
                !startTag(descriptor, "projectOwnerImage").contains("BOTTOM_RIGHT"));
        assertTrue("Аватар должен быть выровнен по центру",
                startTag(descriptor, "projectOwnerImage").contains("align=\"MIDDLE_CENTER\""));

        // Логотип и аватар идут по порядку в hbox (логотип первым).
        assertTrue("Порядок изображений нарушен",
                box.indexOf("id=\"projectLogoImage\"") < box.indexOf("id=\"projectOwnerImage\""));
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
