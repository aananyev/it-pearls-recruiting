package com.company.hunttech.core;

import org.junit.Test;

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
 * Защищает presentation-контракт Edit-формы «Проект» HRM HuntTech
 * (ProjectEdit): sidebar 270px (контракт §4.2), общие edit-* и label-*
 * stylename, единый стиль полей edit-form-control, полоса-заголовок навигации
 * и сохранённые data bindings/loaders/actions. Бизнес-логика и loaders
 * не проверяются.
 */
public class ProjectEditLayoutContractTest {

    private static final String SCREEN =
            "modules/web/src/com/company/hunttech/web/screens/project/project-edit.xml";
    private static final String[] THEMES = {
            "halo",
            "havana",
            "helium",
            "hover",
            "hunttech-modern",
            "hunttech-modern-light",
            "hunttech-modern-dark"
    };

    @Test
    public void usesSharedSidebarAndWorkspaceOrder() throws IOException {
        String xml = readProjectFile(SCREEN);

        assertTrue(xml.contains("stylename=\"edit-sidebar\""));
        assertTrue("sidebar не 270px (контракт §4.2)",
                xml.contains("width=\"270px\""));
        assertTrue(xml.contains("stylename=\"edit-screen-layout\""));
        assertTrue(xml.contains("stylename=\"edit-workspace\""));
        assertTrue(xml.contains("stylename=\"label-navigation\""));
        assertTrue(xml.contains("label-nav-title"));
        assertTrue("нет активного пункта по умолчанию",
                xml.contains("label-nav-item label-nav-item-active"));
        assertTrue(xml.contains("stylename=\"edit-footer-actions\""));
        assertTrue(xml.contains("stylename=\"edit-toolbar\""));
        assertTrue("вкладки без контрактного edit-tabs",
                xml.contains("stylename=\"edit-tabs\""));
        // Порядок идентификации как эталон IteractionListEdit: название (title)
        // СВЕРХУ, подпись типа (subtitle) СНИЗУ.
        int titleIdx = xml.indexOf("id=\"projectSidebarTitle\"");
        int subtitleIdx = xml.indexOf("stylename=\"edit-sidebar-subtitle\"");
        assertTrue("нет title в identity", titleIdx >= 0);
        assertTrue("нет subtitle в identity", subtitleIdx >= 0);
        assertTrue("порядок не эталонный: subtitle раньше title",
                titleIdx < subtitleIdx);
        // Нижние действия: primary «Сохранить и закрыть» и secondary «Отмена».
        assertTrue(xml.contains("stylename=\"project-editor-primary-action\""));
        assertTrue(xml.contains("stylename=\"project-editor-secondary-action\""));
    }

    @Test
    public void usesContractCardAndToolbarClasses() throws IOException {
        String xml = readProjectFile(SCREEN);

        assertTrue("legacy-класс edit-section-card остался",
                !xml.contains("edit-section-card"));
        assertTrue("legacy-класс edit-toolbar-subtitle остался",
                !xml.contains("edit-toolbar-subtitle"));
        assertTrue(xml.contains("stylename=\"edit-card\""));
        assertTrue(xml.contains("stylename=\"edit-toolbar-description\""));
        // Карточки groupBox рендерятся как Vaadin Panel (v-panel-caption), иначе
        // CUBA-рендер c-groupbox-caption не матчит SCSS-правила контракта.
        assertTrue("edit-card без showAsPanel (заголовок карточки не стилизуется)",
                xml.contains("showAsPanel=\"true\""));
        // Полоса-заголовок навигации «Разделы» (контракт §4.1): класс секции
        // поверх label-nav-title — две горизонтальные inset-линии.
        assertTrue("нет полосы-заголовка project-editor-navigation-title",
                xml.contains("label-nav-title project-editor-navigation-title"));
        // Пять контрактных карточек формы.
        assertTrue(xml.contains("id=\"projectMainCard\""));
        assertTrue(xml.contains("id=\"projectChatCard\""));
        assertTrue(xml.contains("id=\"projectDescriptionCard\""));
        assertTrue(xml.contains("id=\"projectVacancyCard\""));
        assertTrue(xml.contains("id=\"projectTemplateCard\""));
    }

    @Test
    public void everyInputFieldUsesEditFormControl() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Каждый типовой input несёт общий stylename контракта (атрибуты XML
        // многострочные — проверяем id и stylename по отдельности, в пределах
        // 600 символов от открывающего тега поля).
        String[] controls = {"projectNameField", "highLevelProjectLookupPickerField",
                "startProjectDateField", "endProjectDateField",
                "projectDepartmentField", "projectOwnerField",
                "generalChatTextField", "chatForCVTextField",
                "projectDescriptionRichTextArea", "templateLetterRichTextArea"};
        for (String id : controls) {
            int idx = xml.indexOf("id=\"" + id + "\"");
            assertTrue(id + ": поле не найдено", idx >= 0);
            int stylenameIdx = xml.indexOf("stylename=\"edit-form-control\"", idx);
            assertTrue(id + ": нет edit-form-control рядом с полем",
                    stylenameIdx > idx && stylenameIdx - idx < 600);
        }

        // Captions полей сохранены.
        assertTrue(xml.contains("caption=\"msg://msgProjectName\""));
        assertTrue(xml.contains("caption=\"msg://msgProjectTree\""));
        assertTrue(xml.contains("caption=\"msg://msgProjectStartDate\""));
        assertTrue(xml.contains("caption=\"msg://msgProjectEndDate\""));
        assertTrue(xml.contains("caption=\"msg://msgProjectDepartament\""));
        assertTrue(xml.contains("caption=\"msg://msgProjectOwner\""));
        assertTrue(xml.contains("caption=\"msg://msgGeneralChat\""));
        assertTrue(xml.contains("caption=\"msg://msgChatForCV\""));
    }

    @Test
    public void dataBindingsViewsLoadersAndActionsPreserved() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Контейнеры и views.
        assertTrue(xml.contains("id=\"projectDc\""));
        assertTrue(xml.contains("view=\"project-edit-view\""));
        assertTrue(xml.contains("id=\"projectOpenPositionsDc\""));
        assertTrue(xml.contains("view=\"openPosition-project-tab-view\""));
        assertTrue(xml.contains("id=\"projectTreeDc\""));
        assertTrue(xml.contains("view=\"project-tree-picker-view\""));
        assertTrue(xml.contains("id=\"projectDepartmentsDc\""));
        assertTrue(xml.contains("view=\"companyDepartament-picker-view\""));
        assertTrue(xml.contains("id=\"projectOwnersDc\""));
        assertTrue(xml.contains("view=\"person-picker-view\""));

        // JPQL-запросы не изменились.
        assertTrue(xml.contains("select e from hunttech_OpenPosition e"));
        assertTrue(xml.contains("order by e.createTs desc"));
        assertTrue(xml.contains("select e from hunttech_Project e"));
        assertTrue(xml.contains("select e from hunttech_CompanyDepartament e"));
        assertTrue(xml.contains("select e from hunttech_Person e"));

        // Property bindings полей.
        assertTrue(xml.contains("property=\"projectIsClosed\""));
        assertTrue(xml.contains("property=\"defaultProject\""));
        assertTrue(xml.contains("property=\"projectTree\""));
        assertTrue(xml.contains("property=\"projectName\""));
        assertTrue(xml.contains("property=\"startProjectDate\""));
        assertTrue(xml.contains("property=\"endProjectDate\""));
        assertTrue(xml.contains("property=\"projectDepartment\""));
        assertTrue(xml.contains("property=\"projectOwner\""));
        assertTrue(xml.contains("property=\"generalChat\""));
        assertTrue(xml.contains("property=\"chatForCV\""));
        assertTrue(xml.contains("property=\"projectDescription\""));
        assertTrue(xml.contains("property=\"templateLetter\""));
        assertTrue(xml.contains("property=\"projectLogo\""));

        // Логотип: единый OvaFallbackImage по эталону JobCandidateEdit (176×176,
        // ovalWidth/ovalHeight, fallback no-company.png, SCALE_DOWN) + загрузчик в sidebar.
        assertTrue("Нет OvaFallbackImage логотипа",
                xml.contains("<ovaFallbackImage id=\"projectLogoFileImage\""));
        assertTrue("OvaFallbackImage не 176×176 (width/height/ovalWidth/ovalHeight)",
                xml.contains("width=\"176px\"") && xml.contains("height=\"176px\"")
                        && xml.contains("ovalWidth=\"176px\"") && xml.contains("ovalHeight=\"176px\""));
        assertTrue("Нет fallbackThemePath icons/no-company.png",
                xml.contains("fallbackThemePath=\"icons/no-company.png\""));
        assertTrue("scaleMode не SCALE_DOWN (эталон IteractionListEdit)",
                xml.contains("scaleMode=\"SCALE_DOWN\""));
        assertTrue("OvaFallbackImage не привязан к projectLogo",
                xml.contains("property=\"projectLogo\""));
        assertFalse("Дублирующий fallback-image projectDefaultLogoFileImage остался",
                xml.contains("projectDefaultLogoFileImage"));
        assertTrue(xml.contains("id=\"projectLogoFileUpload\""));
        assertTrue(xml.contains("fileStoragePutMode=\"IMMEDIATE\""));
        assertTrue(xml.contains("showClearButton=\"true\""));
        assertTrue("dropZone не указывает на visual-блок sidebar",
                xml.contains("dropZone=\"projectEditorSidebarVisual\""));
        // Превью логотипа 96×96 по канону контракта §4.1.
        assertTrue(xml.contains("id=\"projectLogoPicBox\""));

        // Actions и ленивые вкладки (имена — контракт контроллера).
        assertTrue(xml.contains("type=\"picker_lookup\""));
        assertTrue(xml.contains("action=\"windowCommitAndClose\""));
        assertTrue(xml.contains("action=\"windowClose\""));
        assertTrue(xml.contains("id=\"tabProject\""));
        assertTrue(xml.contains("id=\"tabProjectDescription\""));
        assertTrue(xml.contains("id=\"tabVacansy\""));
        assertTrue(xml.contains("id=\"tabTemplateLetter\""));
        assertTrue(xml.contains("id=\"projectTab\""));

        // Навигационные кнопки sidebar = 4 вкладки TabSheet (указание владельца),
        // focusComponent окна.
        assertTrue(xml.contains("id=\"projectEditorNavMain\""));
        assertTrue(xml.contains("id=\"projectEditorNavDescription\""));
        assertTrue(xml.contains("id=\"projectEditorNavVacancy\""));
        assertTrue(xml.contains("id=\"projectEditorNavTemplate\""));
        assertTrue(xml.contains("caption=\"msg://msgProjectName\""));
        assertTrue(xml.contains("caption=\"msg://msgProjectDescription\""));
        assertTrue(xml.contains("caption=\"msg://msgTabVacancy\""));
        assertTrue(xml.contains("caption=\"msg://msgTemplateLetter\""));
        assertFalse("навигация по карточкам вкладки убрана",
                xml.contains("projectEditorNavChats")
                        || xml.contains("msgProjectMainSection")
                        || xml.contains("msgProjectChatSection"));
        assertTrue(xml.contains("focusComponent=\"projectNameField\""));

        // Java: клик по пункту навигации переключает вкладку TabSheet,
        // активный пункт синхронизируется по SelectedTabChange.
        String java = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/project/ProjectEdit.java");
        assertTrue(java.contains("projectTab.setSelectedTab(\"tabProject\")"));
        assertTrue(java.contains("projectTab.setSelectedTab(\"tabProjectDescription\")"));
        assertTrue(java.contains("projectTab.setSelectedTab(\"tabVacansy\")"));
        assertTrue(java.contains("projectTab.setSelectedTab(\"tabTemplateLetter\")"));
        assertTrue(java.contains("TAB_TO_NAV_BUTTON"));
        assertFalse(java.contains("projectEditorNavChats"));
    }

    @Test
    public void everyThemeAppliesProjectLocalScss() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/project-editor.scss");
        assertTrue("Канон SCSS пуст или не содержит mixin",
                canon.contains("@mixin project-editor-theme"));
        assertTrue("Нет фирменного тёмного фона #172638", canon.contains("#172638"));
        assertTrue("Нет канонического active #ffb11b", canon.contains("#ffb11b"));
        // Sidebar 270px с внутренними отступами 14px 16px 12px (контракт §4.2).
        assertTrue("Нет ширины sidebar 270px", canon.contains("width: 270px !important"));
        assertTrue("Нет внутренних отступов sidebar 14px 16px 12px",
                canon.contains("padding: 14px 16px 12px !important"));
        // Название (title) жёлтое #ffb11b 18px по центру — эталон.
        assertTrue("title не жёлтый 18px", canon.contains("color: #ffb11b !important")
                && canon.contains("font-size: 18px !important"));
        // Подпись типа (subtitle) 12px/400 — эталон.
        assertTrue("subtitle не 12px/400", canon.contains("font-size: 12px !important")
                && canon.contains("font-weight: 400 !important"));
        assertTrue("Нет канонического hover rgba(255,255,255,0.08)",
                canon.contains("rgba(255, 255, 255, 0.08)"));
        assertTrue("Нет канонического активного фона rgba(255,177,27,0.12)",
                canon.contains("rgba(255, 177, 27, 0.12)"));
        // Пункты навигации строго 27px.
        assertTrue("nav-кнопки не 27px", canon.contains("height: 27px !important"));
        assertTrue("nav-кнопки не центрированы", canon.contains("align-items: center !important"));
        // halo-тема добавляет кнопке :before — отключаем (иначе подсветка выше текста).
        assertTrue("nav-кнопка не отключает :before halo-темы",
                canon.contains(".label-nav-item:before"));
        assertTrue(":before отключён не display:none",
                canon.contains("display: none !important"));
        assertTrue(":before отключён не content:none",
                canon.contains("content: none !important"));
        // Заголовок toolbar 20px — эталон.
        assertTrue("toolbar title не 20px", canon.contains("font-size: 20px !important"));
        // Нижняя панель: отступы 11px 20px, кнопки 14px/600 высотой 40px.
        assertTrue("footer не 11px 20px", canon.contains("padding: 11px 20px !important"));
        assertTrue("нет primary-кнопки", canon.contains(".project-editor-primary-action"));
        assertTrue("нет secondary-кнопки", canon.contains(".project-editor-secondary-action"));
        // Полоса-заголовок навигации «Разделы» (контракт §4.1): две inset-линии.
        assertTrue("Нет правила полосы-заголовка .project-editor-navigation-title",
                canon.contains(".project-editor-navigation-title"));
        assertTrue("Нет inset-линий полосы-заголовка (box-shadow)",
                canon.contains("rgba(255, 255, 255, 1) 0 1px 0 0 inset"));
        assertTrue("Нет разделителя полосы-заголовка (border-bottom)",
                canon.contains("border-bottom: 1px solid rgba(255, 255, 255, 0.14)"));
        // Визуальный блок sidebar: круглый логотип 96×96 без рамки/тени.
        assertTrue("Нет блока .edit-sidebar-visual", canon.contains(".edit-sidebar-visual"));
        assertTrue("Нет круглой геометрии .project-editor-logo-image",
                canon.contains(".project-editor-logo-image"));
        assertTrue("Нет border-radius 50% у логотипа",
                canon.contains("border-radius: 50% !important"));
        // Вкладки TabSheet оформляются ОБЩИМИ стилями тем (эталон OpenPositionEdit),
        // см. tabsStylesLiveInSharedThemeStyles; в локальном partial дубля нет.
        assertFalse("вкладки не должны дублироваться в project-editor.scss",
                canon.contains(".edit-tabs .v-tabsheet-tabitemcell"));

        for (String theme : THEMES) {
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            assertTrue(theme + ": styles.scss не импортирует project-editor",
                    styles.contains("project-editor"));
            assertTrue(theme + ": styles.scss не вызывает @include project-editor-theme",
                    styles.contains("@include project-editor-theme;"));

            String local = readProjectFile(
                    "modules/web/themes/" + theme + "/com.company.hunttech/project-editor.scss");
            assertTrue("project-editor.scss не идентичен в теме " + theme, canon.equals(local));
        }
    }

    @Test
    public void tabsStylesLiveInSharedThemeStyles() throws IOException {
        // Стили вкладок TabSheet — общие для всех Edit-форм (эталон OpenPositionEdit,
        // перенесены из open-position-editor.scss в edit-screen-shared-styles.scss).
        String sharedCanon = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/edit-screen-shared-styles.scss");
        assertTrue("Нет flex-столбца .edit-tabs", sharedCanon.contains("flex-direction: column !important"));
        assertTrue("Нет полосы вкладок (padding 0 12px, панельный фон)",
                sharedCanon.contains("padding: 0 12px")
                        && sharedCanon.contains("$v-panel-background-color !important")
                        && sharedCanon.contains("border-bottom: 1px solid rgba($v-font-color, 0.15)"));
        assertTrue("Нет nowrap-строки вкладок",
                sharedCanon.contains("flex-wrap: nowrap !important")
                        && sharedCanon.contains("white-space: nowrap !important"));
        assertTrue("Нет подписи вкладки 48px/14px #26384c",
                sharedCanon.contains("height: 48px")
                        && sharedCanon.contains("font-size: 14px !important")
                        && sharedCanon.contains("color: #26384c !important"));
        assertTrue("Нет hover #1264b5", sharedCanon.contains("color: #1264b5 !important"));
        assertTrue("Нет акцентной линии активной вкладки (border-bottom 3px)",
                sharedCanon.contains("border-bottom: 3px solid $v-selection-color !important"));
        assertTrue("Нет панельного фона контента вкладки",
                sharedCanon.contains("padding: 14px 16px 18px")
                        && sharedCanon.contains("mix($v-app-background-color, $v-panel-background-color, 86%) !important"));
        // Идентичность shared-стилей во всех 7 темах.
        String sharedHalo = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/edit-screen-shared-styles.scss");
        for (String theme : THEMES) {
            String s = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/edit-screen-shared-styles.scss");
            assertEquals(theme + ": edit-screen-shared-styles.scss не идентичен halo",
                    sharedHalo, s);
        }
    }

    @Test
    public void uploadButtonsFollowCanonicalDarkSidebarStyle() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/project-editor.scss");

        // Кнопки «Загрузить»/«Очистить» загрузчика логотипа — канон контракта §4.1:
        // пара 96×36, полупрозрачный белый фон, центрирование в блоке визуала.
        assertTrue("Нет растяжения .c-fileupload-wrapper на ширину блока",
                canon.contains(".edit-sidebar-visual .c-fileupload-wrapper"));
        assertTrue("Wrapper не block", canon.contains("display: block !important"));
        assertTrue("Нет .c-fileupload-container в SCSS",
                canon.contains(".edit-sidebar-visual .c-fileupload-container"));
        assertTrue("Нет flex-центрирования пары",
                canon.contains("display: flex !important")
                        && canon.contains("justify-content: center"));
        assertTrue("Нет зазора пары 10px", canon.contains("gap: 10px"));
        // Кнопки: строго 96×36, светлый текст 14px/600, полупрозрачный белый фон.
        assertTrue("Нет ширины кнопки 96px", canon.contains("width: 96px !important"));
        assertTrue("Нет высоты кнопки 36px", canon.contains("min-height: 36px !important"));
        assertTrue("Нет светлого текста #f8fafc", canon.contains("color: #f8fafc !important"));
        assertTrue("Нет шрифта 14px/600", canon.contains("font-size: 14px !important")
                && canon.contains("font-weight: 600 !important"));
        assertTrue("Нет полупрозрачного фона кнопки",
                canon.contains("background: rgba(255, 255, 255, 0.06) !important"));
        assertTrue("Нет рамки кнопки",
                canon.contains("border: 1px solid rgba(255, 255, 255, 0.34) !important"));
        assertTrue("Нет скругления 5px", canon.contains("border-radius: 5px !important"));
        assertTrue("Кнопка сохранила вало-тень",
                canon.contains("box-shadow: none !important"));
        assertTrue("Нет правила .c-fileupload-clear",
                canon.contains(".edit-sidebar-visual .c-fileupload-clear"));
        assertTrue("Нет hover-фона кнопок",
                canon.contains("rgba(255, 255, 255, 0.12) !important"));
    }

    @Test
    public void visualContractFollowsIteractionListEditReference() throws IOException {
        String xml = readProjectFile(SCREEN);
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/project-editor.scss");

        // Sidebar отделён правой границей и тенью (эталон iteraction-list-sidebar).
        assertTrue("Нет border-right sidebar",
                canon.contains("border-right: 1px solid rgba(15, 23, 42, 0.78)"));
        assertTrue("Нет тени sidebar 5px 0 20px",
                canon.contains("box-shadow: 5px 0 20px rgba(15, 23, 42, 0.18)"));
        // Визуальный блок sidebar: min-height 104px (эталон identity-images).
        assertTrue("visual-блок не 104px", canon.contains("min-height: 104px"));
        // Spacer sidebar на всю высоту (атрибуты многострочные — по отдельности).
        assertTrue("projectSidebarSpacer отсутствует",
                xml.contains("id=\"projectSidebarSpacer\""));
        assertTrue("projectSidebarSpacer без height=100%",
                xml.contains("id=\"projectSidebarSpacer\"")
                        && xml.contains("stylename=\"edit-sidebar-spacer\"")
                        && xml.contains("height=\"100%\""));
        // Footer: верхняя тень, hover-эффект, expand-спейсер + группа AUTO/MIDDLE_RIGHT.
        assertTrue("Нет верхней тени footer",
                canon.contains("box-shadow: 0 -2px 8px rgba(15, 23, 42, 0.04)"));
        assertTrue("Нет hover footer-кнопок", canon.contains("filter: brightness(0.98)"));
        assertTrue("editActions без expand-спейсера",
                xml.contains("expand=\"bottomActionsSpacer\""));
        assertTrue("Нет группы bottomActionsGroup",
                xml.contains("id=\"bottomActionsGroup\""));
        assertTrue("Группа footer без width=AUTO",
                xml.contains("id=\"bottomActionsGroup\"")
                        && xml.contains("width=\"AUTO\"")
                        && xml.contains("align=\"MIDDLE_RIGHT\""));
        assertTrue("Спейсер footer без height=1px",
                xml.contains("id=\"bottomActionsSpacer\"")
                        && xml.contains("height=\"1px\""));
        // Полноэкранный модальный диалог (контракт §5.3).
        assertTrue("Нет dialogMode 100%×100%",
                xml.contains("height=\"100%\"") && xml.contains("width=\"100%\"")
                        && xml.contains("modal=\"true\""));
        // Вкладка «Проект»: прокручиваемый контент edit-workspace-content.
        assertTrue("Вкладка проекта без scrollBox edit-workspace-scroll",
                xml.contains("stylename=\"edit-workspace edit-workspace-scroll\""));
        assertTrue("Нет контента вкладки edit-workspace-content",
                xml.contains("stylename=\"edit-workspace-content\""));
        // Подсказка sidebar.
        assertTrue("Нет hint sidebar",
                xml.contains("stylename=\"edit-sidebar-hint\""));
    }

    @Test
    public void mainTabInputsSpanFullWidthAndTabsDoNotOverflow() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Вкладка «Проект»: все элементы ввода растянуты на ширину страницы (100%),
        // в карточке «Основные данные» не осталось полей на 50%.
        assertFalse("В форме остались поля width=50%", xml.contains("width=\"50%\""));
        String[] fullWidthInputs = {"highLevelProjectLookupPickerField", "projectNameField",
                "projectDepartmentField", "projectOwnerField"};
        for (String id : fullWidthInputs) {
            int idx = xml.indexOf("id=\"" + id + "\"");
            assertTrue(id + ": поле не найдено", idx >= 0);
            int widthIdx = xml.indexOf("width=\"100%\"", idx);
            assertTrue(id + ": ширина не 100%", widthIdx > idx && widthIdx - idx < 700);
        }
        // Строка дат растянута на всю ширину, оба поля дат делят строку поровну.
        assertTrue("Нет строки дат hbox width=100%",
                xml.contains("<hbox width=\"100%\" spacing=\"true\">"));
        for (String id : new String[]{"startProjectDateField", "endProjectDateField"}) {
            int idx = xml.indexOf("id=\"" + id + "\"");
            int widthIdx = xml.indexOf("width=\"100%\"", idx);
            assertTrue(id + ": ширина не 100%", widthIdx > idx && widthIdx - idx < 700);
            // Обе даты на одной строке одинакового размера: box.expandRatio делит hbox 50/50
            // (два width=100% без expandRatio выталкивали вторую дату за границы).
            int ratioIdx = xml.indexOf("box.expandRatio=\"1\"", idx);
            assertTrue(id + ": нет box.expandRatio=1", ratioIdx > widthIdx && ratioIdx - widthIdx < 300);
        }

        // RichTextArea и dataGrid не выходят за границы: локальный SCSS ограничивает
        // ширину flex-контейнеров (min-width:auto — источник переполнения).
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/project-editor.scss");
        assertTrue("Нет ограничения .v-richtextarea (min-width: 0)",
                canon.contains(".v-richtextarea {")
                        && canon.contains("min-width: 0 !important"));
        assertTrue("Нет ограничения iframe RichTextArea (max-width: 100%)",
                canon.contains(".v-richtextarea .v-richtextarea-content")
                        && canon.contains("max-width: 100%;"));
        assertTrue("Нет ограничения .v-grid (min-width: 0 / max-width: 100%)",
                canon.contains(".v-grid {")
                        && canon.contains("max-width: 100% !important"));
    }

    @Test
    public void shortDescriptionSidebarSectionContract() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Раздел «Коротко» sidebar: контейнер между идентификацией и навигацией,
        // заголовок и текст — отдельные label'ы с локальными stylename.
        assertTrue("Нет контейнера раздела «Коротко»",
                xml.contains("id=\"projectEditorSidebarShortDescription\""));
        assertTrue("Раздел не скрыт по умолчанию (visible=false)",
                xml.contains("visible=\"false\""));
        assertTrue("Нет заголовка раздела",
                xml.contains("id=\"projectSidebarShortDescriptionTitle\""));
        assertTrue("Заголовок без msg-ключа",
                xml.contains("value=\"msg://msgProjectShortDescriptionSection\""));
        assertTrue("Нет текста раздела",
                xml.contains("id=\"projectSidebarShortDescriptionText\""));
        assertTrue("Заголовок без stylename project-editor-short-description-title",
                xml.contains("stylename=\"project-editor-short-description-title\""));
        assertTrue("Текст без stylename project-editor-short-description-text",
                xml.contains("stylename=\"project-editor-short-description-text\""));

        // Раздел расположен между идентификацией (identity) и навигацией («Разделы»).
        int identityIdx = xml.indexOf("id=\"projectEditorSidebarIdentity\"");
        int sectionIdx = xml.indexOf("id=\"projectEditorSidebarShortDescription\"");
        int navIdx = xml.indexOf("id=\"projectEditorSidebarNavigation\"");
        assertTrue("identity отсутствует", identityIdx >= 0);
        assertTrue("навигация отсутствует", navIdx >= 0);
        assertTrue("раздел не между identity и навигацией",
                identityIdx < sectionIdx && sectionIdx < navIdx);
    }

    @Test
    public void shortDescriptionScssStylesInCanon() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/project-editor.scss");

        // Заголовок раздела «Коротко»: подпись 11px/700 uppercase.
        assertTrue("Нет стиля заголовка .project-editor-short-description-title",
                canon.contains(".project-editor-short-description-title"));
        assertTrue("Заголовок не 11px/700",
                canon.contains("font-size: 11px !important")
                        && canon.contains("font-weight: 700 !important"));
        // Текст раздела: 13px/500, переносы включены.
        assertTrue("Нет стиля текста .project-editor-short-description-text",
                canon.contains(".project-editor-short-description-text"));
        assertTrue("Текст не 13px/500",
                canon.contains("font-size: 13px !important")
                        && canon.contains("font-weight: 500 !important"));
        assertTrue("Текст без переноса overflow-wrap",
                canon.contains("overflow-wrap: anywhere"));
        // Кнопка «Кратко» в рабочей области.
        assertTrue("Нет стиля кнопки .project-editor-short-description-button",
                canon.contains(".project-editor-short-description-button"));
        assertTrue("Кнопка не 38px",
                canon.contains("height: 38px !important")
                        && canon.contains("min-height: 38px !important"));
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
