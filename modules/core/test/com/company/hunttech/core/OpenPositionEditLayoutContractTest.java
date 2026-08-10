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
                "commandOrVacancyGroupBox",
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

        // collapsable/collapsed legacy-контракта сохранены; projectTypeGroupBox и
        // personnelCountGroupBox удалены редизайном (тип проекта — в openPositionEditorIdentifiersCard,
        // количество персонала — в параметры вакансии), procActionsBox legacy-контракт
        // не имел collapsable — не проверяется.
        String[] collapsableIds = {
                "commandOrVacancyGroupBox",
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
        // фиксированной ширины (ID и «Генерировать» — 110px, Грейд — 143px = 110 + 30%),
        // Вакансия растягивается на свободное место через box.expandRatio; expand на hbox не используется.
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
        assertTrue("Грейд должен иметь ширину 143px (110px + 30%)",
                gradeTag.contains("width=\"143px\""));
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

        // Набор навигации вкладки «Вакансия» (openPositionMainTabNavigation) содержит ровно
        // 5 пунктов в порядке следования секций формы: «Наименование», «Параметры вакансии»,
        // «Вакансия», «Команда», «Зарплатное предложение».
        int navStart = descriptor.indexOf("id=\"openPositionMainTabNavigation\"");
        assertTrue("Набор навигации вкладки «Вакансия» не найден", navStart >= 0);
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

        // Подписи пунктов соответствуют блокам вкладки (в пределах открывающего тега кнопки).
        assertTrue("«Наименование» должен использовать mainMsg://msgName",
                section(nav, "id=\"openPositionEditorNavName\"", "/>")
                        .contains("caption=\"mainMsg://msgName\""));
        assertTrue("«Параметры вакансии» должен использовать msg://msgVacancyParams",
                section(nav, "id=\"openPositionEditorNavParams\"", "/>")
                        .contains("caption=\"msg://msgVacancyParams\""));
        assertTrue("«Вакансия» должен использовать mainMsg://openPositionEditorNavVacancy",
                section(nav, "id=\"openPositionEditorNavVacancy\"", "/>")
                        .contains("caption=\"mainMsg://openPositionEditorNavVacancy\""));
        assertTrue("«Команда» должен использовать msg://msgCommand",
                section(nav, "id=\"openPositionEditorNavTeam\"", "/>")
                        .contains("caption=\"msg://msgCommand\""));
        assertTrue("«Зарплатное предложение» должен использовать msg://msgSalaryProposal",
                section(nav, "id=\"openPositionEditorNavSalary\"", "/>")
                        .contains("caption=\"msg://msgSalaryProposal\""));

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
        // Вложенный @media (max-height: 800px) — легитимный компактный режим sidebar
        // (задокументирован в истории UI Spec); CUBA Sass его компилирует.
        assertTrue("Компактный режим @media (max-height: 800px) должен присутствовать",
                referenceLocalScss.contains("@media (max-height: 800px)"));
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

    @Test
    public void sidebarSectionTitlesMatchInfoCardCaptionStyle() throws IOException {
        String descriptor = readProjectFile(EDIT_XML);

        // Оба заголовка секций sidebar несут класс open-position-editor-section-title
        // поверх базового label-nav-title.
        String navSectionsLabel = startTag(descriptor, "openPositionEditorNavActiveSectionsLabel");
        String contextLabel = startTag(descriptor, "openPositionEditorContextLabel");
        assertTrue("«Разделы активной вкладки» не получил класс open-position-editor-section-title",
                navSectionsLabel.contains("label-nav-title open-position-editor-section-title"));
        assertTrue("«Контекст вакансии» не получил класс open-position-editor-section-title",
                contextLabel.contains("label-nav-title open-position-editor-section-title"));

        // Стиль заголовка секции = caption инфокарточки «Информация» (1:1) во всех 7 темах.
        String caption = "    .edit-sidebar .open-position-editor-info-card .v-panel-caption {";
        String sectionTitle = "    .edit-sidebar .open-position-editor-section-title {";
        for (String theme : THEMES) {
            String scss = readProjectFile(themeScssPath(theme));
            assertTrue("В теме " + theme + " нет caption инфокарточки",
                    scss.contains(caption));
            assertTrue("В теме " + theme + " нет класса open-position-editor-section-title",
                    scss.contains(sectionTitle));
            String sectionBlock = section(scss, sectionTitle, "    }\n\n    .edit-sidebar .open-position-editor-info-card .v-panel-content {");
            String captionBlock = section(scss, caption, "    }\n\n    .edit-sidebar .open-position-editor-info-card .v-panel-content {");
            for (String value : Arrays.asList(
                    "min-height: 36px !important;",
                    "padding: 7px 11px !important;",
                    "color: #ffb11b !important;",
                    "font-size: 15px !important;",
                    "font-weight: 700 !important;",
                    "line-height: 21px !important;",
                    "background: rgba(255, 255, 255, 0.045) !important;",
                    "border-bottom: 1px solid rgba(255, 255, 255, 0.14) !important;")) {
                assertTrue("В теме " + theme + " секционный заголовок расходится с caption «Информация»: " + value,
                        sectionBlock.contains(value) && captionBlock.contains(value));
            }
            // Две горизонтальные inset-линии полосы заголовка (белая сверху, светлая снизу) —
            // как у caption «Информации» (там это вало-дефолт v-panel-caption, поэтому
            // в SCSS-caption их нет; у секционных заголовков задаём явно).
            assertTrue("В теме " + theme + " нет белой inset-линии сверху",
                    sectionBlock.contains("rgba(255, 255, 255, 1) 0 1px 0 0 inset"));
            assertTrue("В теме " + theme + " нет светлой inset-линии снизу",
                    sectionBlock.contains("rgba(244, 244, 244, 1) 0 -1px 0 0 inset"));
        }
    }

    @Test
    public void sidebarVacancyContextContainsGradeRegistrationStatus() throws IOException {
        String descriptor = readProjectFile(EDIT_XML);

        // Блок «Контекст вакансии» (openPositionEditorSummary) содержит сводку городов,
        // затем три контекстные строки (грейд, оформление, статус) и итоговые комиссии —
        // все строки используют единый стиль open-position-editor-summary-value.
        String grade = startTag(descriptor, "contextGradeLabel");
        String registration = startTag(descriptor, "contextRegistrationLabel");
        String status = startTag(descriptor, "contextStatusLabel");
        for (String tag : Arrays.asList(grade, registration, status)) {
            assertTrue("Контекстная строка не имеет htmlEnabled",
                    tag.contains("htmlEnabled=\"true\""));
            assertTrue("Контекстная строка не использует open-position-editor-summary-value",
                    tag.contains("open-position-editor-summary-value"));
        }

        // Порядок в блоке: города → грейд → оформление → статус → комиссия рекрутера.
        int cities = descriptor.indexOf("id=\"citiesLabel\"");
        int gradeIdx = descriptor.indexOf("id=\"contextGradeLabel\"");
        int registrationIdx = descriptor.indexOf("id=\"contextRegistrationLabel\"");
        int statusIdx = descriptor.indexOf("id=\"contextStatusLabel\"");
        int recruiterIdx = descriptor.indexOf("id=\"labelTopComissionRecrutier\"");
        assertTrue("Порядок строк «Контекста вакансии» нарушен",
                cities >= 0 && cities < gradeIdx && gradeIdx < registrationIdx
                        && registrationIdx < statusIdx && statusIdx < recruiterIdx);
    }

    @Test
    public void vacancyStatePairMirrorsIteractionListEdit() throws IOException {
        String descriptor = readProjectFile(EDIT_XML);

        // Пара «Статус вакансии» + «Приоритет» — горизонтальная пара 50/50 (эталон
        // IteractionListEdit vacancyStateSummary): hbox с двумя ячейками width=50%.
        // Конец пары — маркер следующей строки (remoteWorkCaptionLabel), т.к. внутри
        // ячеек есть вложенные hbox-строки значений с собственными </hbox>.
        String pair = section(descriptor, "id=\"vacancyStateSummary\"", "id=\"remoteWorkCaptionLabel\"");
        assertTrue("Пара не использует open-position-editor-vacancy-state-summary",
                startTag(descriptor, "vacancyStateSummary")
                        .contains("open-position-editor-vacancy-state-summary"));
        assertTrue("Пара не позиционирована как 50/50 (hbox + spacing)",
                startTag(descriptor, "vacancyStateSummary").contains("spacing=\"true\""));
        assertTrue("Левая ячейка vacancyStatusSummary отсутствует в паре",
                pair.contains("id=\"vacancyStatusSummary\""));
        assertTrue("Правая ячейка vacancyPrioritySummary отсутствует в паре",
                pair.contains("id=\"vacancyPrioritySummary\""));
        assertTrue("Порядок ячеек нарушен (статус → приоритет)",
                pair.indexOf("id=\"vacancyStatusSummary\"") < pair.indexOf("id=\"vacancyPrioritySummary\""));
        for (String cellId : new String[]{"vacancyStatusSummary", "vacancyPrioritySummary"}) {
            String tag = startTag(descriptor, cellId);
            assertTrue("Ячейка " + cellId + " не имеет ширину 50%",
                    tag.contains("width=\"50%\""));
            assertTrue("Ячейка " + cellId + " не имеет рамку-класс vacancy-state-cell",
                    tag.contains("open-position-editor-vacancy-state-cell"));
            assertTrue("Ячейка " + cellId + " не несёт sidebar-info-row",
                    tag.contains("open-position-editor-sidebar-info-row"));
        }

        // Левая ячейка: caption «Статус вакансии» + statusOfVacansyLabel (значение из Java).
        String statusCell = section(descriptor, "id=\"vacancyStatusSummary\"", "</vbox>");
        assertTrue("Caption статуса не использует msgStatusOfVacansy",
                statusCell.contains("value=\"msg://msgStatusOfVacansy\""));
        assertTrue("Caption статуса не использует sidebar-caption",
                section(statusCell, "value=\"msg://msgStatusOfVacansy\"", "/>")
                        .contains("open-position-editor-sidebar-caption"));
        assertTrue("statusOfVacansyLabel отсутствует в ячейке статуса",
                statusCell.contains("id=\"statusOfVacansyLabel\""));
        String statusLabelTag = startTag(descriptor, "statusOfVacansyLabel");
        assertTrue("statusOfVacansyLabel не несёт sidebar-value/status-value",
                statusLabelTag.contains("open-position-editor-sidebar-value")
                        && statusLabelTag.contains("open-position-editor-status-value"));
        assertTrue("Строка значения статуса не имеет vacancyStatusValueBox",
                statusCell.contains("id=\"vacancyStatusValueBox\""));
        assertTrue("Строка значения статуса не имеет sidebar-value-row",
                section(statusCell, "id=\"vacancyStatusValueBox\"", "</hbox>")
                        .contains("open-position-editor-sidebar-value-row"));

        // Правая ячейка: caption «Приоритет» + светофор + currentPriorityLabel.
        String priorityCell = section(descriptor, "id=\"vacancyPrioritySummary\"", "</vbox>");
        assertTrue("Caption приоритета не использует msgPriority",
                priorityCell.contains("value=\"msg://msgPriority\""));
        assertTrue("Светофор trafficLighterImage отсутствует в ячейке приоритета",
                priorityCell.contains("id=\"trafficLighterImage\""));
        assertTrue("currentPriorityLabel отсутствует в ячейке приоритета",
                priorityCell.contains("id=\"currentPriorityLabel\""));
        assertTrue("currentPriorityLabel не несёт sidebar-value",
                startTag(descriptor, "currentPriorityLabel")
                        .contains("open-position-editor-sidebar-value"));

        // Формат работы — отдельная строка под парой (не внутри ячейки).
        String remoteCaptionTag = startTag(descriptor, "remoteWorkCaptionLabel");
        assertTrue("Подпись формата работы не использует sidebar-caption",
                remoteCaptionTag.contains("open-position-editor-sidebar-caption"));
        assertTrue("Подпись формата работы не использует msgRemoteWorkSidebar",
                remoteCaptionTag.contains("value=\"msg://msgRemoteWorkSidebar\""));
        String remoteValueTag = startTag(descriptor, "remoteWorkSidebarLabel");
        assertTrue("Значение формата работы не использует sidebar-value",
                remoteValueTag.contains("open-position-editor-sidebar-value"));
        assertTrue("Удалёнка не должна находиться внутри пары",
                pair.indexOf("id=\"remoteWorkSidebarLabel\"") < 0);
        assertTrue("Старые классы блока приоритета должны быть удалены из XML",
                !descriptor.contains("open-position-editor-priority-summary")
                        && !descriptor.contains("open-position-editor-priority-value-row")
                        && !descriptor.contains("open-position-editor-priority-value\""));
    }

    @Test
    public void vacancyStatePairScssCarriesHighlightFramesInAllThemes() throws IOException {
        String summary = "    .open-position-editor-vacancy-state-summary {";
        String cell = "    .open-position-editor-vacancy-state-cell,";
        String caption = "    .open-position-editor-sidebar-caption,";
        String value = "    .open-position-editor-sidebar-value,";
        String valueRow = "    .open-position-editor-sidebar-value-row {";
        for (String theme : THEMES) {
            String scss = readProjectFile(themeScssPath(theme));
            assertTrue("В теме " + theme + " нет класса пары vacancy-state-summary",
                    scss.contains(summary));
            assertTrue("В теме " + theme + " нет класса рамки vacancy-state-cell",
                    scss.contains(cell));
            assertTrue("В теме " + theme + " нет sidebar-caption",
                    scss.contains(caption));
            assertTrue("В теме " + theme + " нет sidebar-value",
                    scss.contains(value));
            assertTrue("В теме " + theme + " нет sidebar-value-row",
                    scss.contains(valueRow));

            // Рамка выделения ячейки — 1:1 с iteraction-list-vacancy-state-cell эталона.
            String cellBlock = section(scss, cell,
                    "    .open-position-editor-vacancy-state-cell > .v-slot,");
            for (String rule : Arrays.asList(
                    "background: rgba(255, 255, 255, 0.05);",
                    "border: 1px solid rgba(255, 255, 255, 0.12) !important;",
                    "border-radius: 6px;",
                    "padding: 8px 9px !important;")) {
                assertTrue("В теме " + theme + " рамка ячейки не содержит " + rule,
                        cellBlock.contains(rule));
            }
            // Стили caption/value в ячейке — как у эталона (11px caption, 13px value).
            String captionInCell = section(scss,
                    "    .open-position-editor-vacancy-state-cell .open-position-editor-sidebar-caption {",
                    "    .open-position-editor-vacancy-state-cell .open-position-editor-sidebar-value,");
            assertTrue("В теме " + theme + " caption ячейки не 11px",
                    captionInCell.contains("font-size: 11px !important;"));
            String valueInCell = section(scss,
                    "    .open-position-editor-vacancy-state-cell .open-position-editor-sidebar-value,",
                    "    /* Значение в hbox-строке");
            assertTrue("В теме " + theme + " value ячейки не 13px",
                    valueInCell.contains("font-size: 13px !important;")
                            && valueInCell.contains("line-height: 18px !important;"));
        }
    }

    @Test
    public void vacancyCloseButtonSitsUnderStatusPriorityPair() throws IOException {
        String descriptor = readProjectFile(EDIT_XML);

        // Кнопка «Закрыть/Открыть вакансию» расположена сразу под парой (до строки формата работы),
        // как просил владелец: под блоком «Статус вакансии»+«Приоритет».
        assertTrue("Кнопка открытия/закрытия отсутствует",
                descriptor.contains("id=\"openClosePositionButton\""));
        assertTrue("Порядок нарушен: кнопка должна идти после пары",
                descriptor.indexOf("id=\"vacancyStateSummary\"")
                        < descriptor.indexOf("id=\"openClosePositionButton\""));
        assertTrue("Порядок нарушен: кнопка должна идти до строки формата работы",
                descriptor.indexOf("id=\"openClosePositionButton\"")
                        < descriptor.indexOf("id=\"remoteWorkCaptionLabel\""));

        String tag = startTag(descriptor, "openClosePositionButton");
        assertTrue("Кнопка не имеет caption msgCloseVacancy (открыта → «Закрыть вакансию»)",
                tag.contains("caption=\"msg://msgCloseVacancy\""));
        assertTrue("Кнопка не имеет invoke openClosePositionToggle",
                tag.contains("invoke=\"openClosePositionToggle\""));
        assertTrue("Кнопка не растянута на 100% ширины",
                tag.contains("width=\"100%\""));
        assertTrue("Кнопка не использует базовый класс small (как footer JobCandidateEdit)",
                tag.contains("stylename=\"small open-position-editor-open-close-button\""));

        // Preview-контракт: инжектируемая кнопка обязана быть в preview как скрытая заглушка.
        String preview = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit-preview.xml");
        assertTrue("Preview не содержит скрытую заглушку openClosePositionButton",
                preview.contains("id=\"openClosePositionButton\"")
                        && preview.contains("visible=\"false\""));

        // Java-контракт: toggle инвертирует OPEN_CLOSE, надпись переключается по состоянию.
        String controller = readProjectFile(EDIT_CONTROLLER);
        assertTrue("Контроллер не содержит toggle openClosePositionToggle",
                controller.contains("public void openClosePositionToggle()"));
        assertTrue("Контроллер не содержит refreshOpenCloseButton",
                controller.contains("private void refreshOpenCloseButton()"));
        assertTrue("Контроллер не переключает надпись msgOpenVacancy/msgCloseVacancy",
                controller.contains("closed ? \"msgOpenVacancy\" : \"msgCloseVacancy\""));
    }

    @Test
    public void vacancyCloseButtonScssCarriesJobCandidateFooterLookInAllThemes() throws IOException {
        // Визуал кнопки — 1:1 с footer-кнопками JobCandidateEdit
        // (job-candidate-profile-footer .v-button): 100%, min-height 38px, 14/600,
        // фон rgba(255,255,255,.06), border rgba(255,255,255,.34), radius 5px, hover .10.
        String button = "    .open-position-editor-open-close-button {";
        for (String theme : THEMES) {
            String scss = readProjectFile(themeScssPath(theme));
            assertTrue("В теме " + theme + " нет класса кнопки open-close-button",
                    scss.contains(button));
            String block = section(scss, button,
                    "    .open-position-editor-open-close-button:hover {");
            for (String rule : Arrays.asList(
                    "width: 100% !important;",
                    "min-height: 38px;",
                    "color: #f8fafc !important;",
                    "font-size: 14px;",
                    "font-weight: 600;",
                    "background: rgba(255, 255, 255, .06) !important;",
                    "border: 1px solid rgba(255, 255, 255, .34) !important;",
                    "border-radius: 5px;",
                    "box-shadow: none !important;")) {
                assertTrue("В теме " + theme + " кнопка не содержит " + rule,
                        block.contains(rule));
            }
            String hover = section(scss,
                    "    .open-position-editor-open-close-button:hover {",
                    "    }");
            assertTrue("В теме " + theme + " hover кнопки не .10",
                    hover.contains("background: rgba(255, 255, 255, .10) !important;"));
        }
    }

    @Test
    public void tabsCaptionsMatchJobCandidateEditFont() throws IOException {
        // Шрифт заголовков вкладок — 1:1 с эталоном JobCandidateEdit (font 14/600,
        // цвет #26384c, hover #1264b5, selected = $v-selection-color) во всех 7 темах.
        String captionStart = "    .open-position-editor-tabs .v-tabsheet-tabitem .v-caption {";
        String captionEnd = "    .open-position-editor-tabs .v-tabsheet-tabitem:hover .v-caption {";
        String hoverEnd = "    .open-position-editor-tabs .v-tabsheet-tabitem-selected .v-caption {";
        for (String theme : THEMES) {
            String scss = readProjectFile(themeScssPath(theme));
            String captionBlock = section(scss, captionStart, captionEnd);
            for (String value : Arrays.asList(
                    "font-size: 14px !important;",
                    "font-weight: 600;",
                    "color: #26384c !important;",
                    "line-height: 48px;",
                    "padding: 0 10px;")) {
                assertTrue("В теме " + theme + " caption вкладок расходится с эталоном JobCandidateEdit: " + value,
                        captionBlock.contains(value));
            }
            String hoverBlock = section(scss, captionEnd, hoverEnd);
            assertTrue("В теме " + theme + " hover вкладок не совпадает с JobCandidateEdit (#1264b5)",
                    hoverBlock.contains("color: #1264b5 !important;"));
            assertTrue("В теме " + theme + " selected вкладок должен использовать $v-selection-color",
                    section(scss, hoverEnd, "    .open-position-editor-tabs .v-tabsheet-content {")
                            .contains("color: $v-selection-color !important;"));
        }
    }

    @Test
    public void sidebarNavigationSetsFollowTabs() throws IOException {
        // Label-навигация sidebar — наборы по вкладкам tabSheetOpenPosition: контейнер
        // openPositionEditorNavigation содержит заголовок и 11 вложенных vbox-наборов
        // openPosition*TabNavigation; все кроме вкладки по умолчанию скрыты; каждая кнопка
        // набора — borderless label-nav-item. Java: syncSidebarNavigation показывает набор
        // только для вкладок с двумя и более блоками ввода (TABS_WITH_SIDEBAR_NAVIGATION),
        // одноблочные наборы всегда setVisible(false), контейнер скрывается целиком.
        String descriptor = readProjectFile(EDIT_XML);
        String controller = readProjectFile(EDIT_CONTROLLER);

        List<String> multiBlockSets = Arrays.asList(
                "openPositionMainTabNavigation",
                "openPositionLaborTabNavigation",
                "openPositionJobDescriptionTabNavigation");
        List<String> singleBlockSets = Arrays.asList(
                "openPositionFilesTabNavigation",
                "openPositionExerciseTabNavigation",
                "openPositionMemoTabNavigation",
                "openPositionTemplateLetterTabNavigation",
                "openPositionSkillsTabNavigation",
                "openPositionNewsTabNavigation",
                "openPositionApprovalTabNavigation",
                "openPositionCommentsTabNavigation");

        // Контейнер навигации: существует в XML, Java управляет его видимостью целиком.
        assertTrue("XML: контейнер openPositionEditorNavigation отсутствует",
                descriptor.contains("id=\"openPositionEditorNavigation\""));
        assertTrue("Java: syncSidebarNavigation не переключает контейнер openPositionEditorNavigation",
                controller.contains("openPositionEditorNavigation.setVisible("));
        assertTrue("Java: не задекларирован TABS_WITH_SIDEBAR_NAVIGATION (вкладки с 2+ блоками)",
                controller.contains("TABS_WITH_SIDEBAR_NAVIGATION"));

        for (String setId : multiBlockSets) {
            assertTrue("XML: набор навигации " + setId + " отсутствует", descriptor.contains("id=\"" + setId + "\""));
            assertTrue("Java: syncSidebarNavigation не переключает набор " + setId,
                    controller.contains(setId + ".setVisible("));
            if (!"openPositionMainTabNavigation".equals(setId)) {
                String block = section(descriptor, "<vbox id=\"" + setId + "\"", "</vbox>");
                assertTrue("XML: набор " + setId + " должен быть скрыт по умолчанию (visible=\"false\")",
                        block.contains("visible=\"false\""));
            }
        }
        for (String setId : singleBlockSets) {
            assertTrue("XML: набор навигации " + setId + " отсутствует", descriptor.contains("id=\"" + setId + "\""));
            // Одноблочные вкладки: набор никогда не показывается (контейнер скрыт целиком).
            assertTrue("Java: набор " + setId + " должен быть всегда setVisible(false)",
                    controller.contains(setId + ".setVisible(false)"));
            String block = section(descriptor, "<vbox id=\"" + setId + "\"", "</vbox>");
            assertTrue("XML: набор " + setId + " должен быть скрыт по умолчанию (visible=\"false\")",
                    block.contains("visible=\"false\""));
        }

        // Все навигационные кнопки (18) — borderless label-nav-item и имеют Java-поле.
        for (String buttonId : new String[]{
                "openPositionEditorNavName", "openPositionEditorNavParams", "openPositionEditorNavVacancy",
                "openPositionEditorNavTeam", "openPositionEditorNavSalary", "openPositionEditorNavLaborAgreement",
                "openPositionEditorNavPaymentsDetail", "openPositionEditorNavWorkExperience",
                "openPositionEditorNavDescription", "openPositionEditorNavShortDescription",
                "openPositionEditorNavFiles", "openPositionEditorNavExercise", "openPositionEditorNavMemo",
                "openPositionEditorNavTemplateLetter", "openPositionEditorNavSkills", "openPositionEditorNavNews",
                "openPositionEditorNavApproval", "openPositionEditorNavComments"}) {
            String buttonBlock = section(descriptor, "<button id=\"" + buttonId + "\"", "/>");
            assertTrue("XML: кнопка " + buttonId + " должна быть borderless label-nav-item",
                    buttonBlock.contains("borderless label-nav-item"));
            assertTrue("Java: кнопка " + buttonId + " не задекларирована как поле",
                    controller.contains("private Button " + buttonId + ";"));
        }
    }

    private String themeScssPath(String theme) {
        return "modules/web/themes/" + theme + "/com.company.hunttech/" + LOCAL_STYLE;
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
