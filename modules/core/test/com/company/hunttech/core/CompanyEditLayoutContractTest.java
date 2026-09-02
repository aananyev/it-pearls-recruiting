package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Защищает presentation-контракт Edit-формы «Компании» HRM HuntTech
 * (CompanyEdit): sidebar 270px (контракт §4.2), общие edit-* и label-*
 * stylename, единый стиль полей edit-form-control, полоса-заголовок навигации
 * «Разделы» (пункты = вкладки TabSheet, эталон ProjectEdit), логотип
 * OvaFallbackImage 176×176 и сохранённые data bindings/loaders/actions.
 * Бизнес-логика и loaders не проверяются.
 */
public class CompanyEditLayoutContractTest {

    private static final String SCREEN =
            "modules/web/src/com/company/hunttech/web/screens/company/company-edit.xml";
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
        assertFalse("tabSheet остался framed (ломает контрактный вид вкладок)",
                xml.contains("stylename=\"framed\""));
        // Полноэкранный модальный режим (контракт §5.3).
        assertTrue(xml.contains("height=\"100%\""));
        assertTrue(xml.contains("width=\"100%\""));
        assertTrue(xml.contains("modal=\"true\""));
        // Идентификация: title по центру; подпись типа записи не используется
        // (канон серии Edit-форм 2026-08-14).
        int titleIdx = xml.indexOf("id=\"companySidebarTitle\"");
        assertTrue("нет title в identity", titleIdx >= 0);
        assertFalse("подпись типа записи (subtitle) осталась в identity",
                xml.contains("stylename=\"edit-sidebar-subtitle\""));
        // Нижние действия: primary «Сохранить и закрыть» и secondary «Отмена».
        assertTrue(xml.contains("stylename=\"company-editor-primary-action\""));
        assertTrue(xml.contains("stylename=\"company-editor-secondary-action\""));
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
        // Полоса-заголовок навигации «Разделы» (контракт §4.1).
        assertTrue("нет полосы-заголовка company-editor-navigation-title",
                xml.contains("label-nav-title company-editor-navigation-title"));
        // Четыре контрактных карточки формы.
        assertTrue(xml.contains("id=\"companyMainCard\""));
        assertTrue(xml.contains("id=\"companyAddressCard\""));
        assertTrue(xml.contains("id=\"companyDescriptionCard\""));
        assertTrue(xml.contains("id=\"companyDepartmentsCard\""));
    }

    @Test
    public void everyInputFieldUsesEditFormControl() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Каждый типовой input несёт общий stylename контракта (атрибуты XML
        // многострочные — проверяем id и stylename по отдельности, в пределах
        // 600 символов от открывающего тега поля).
        String[] controls = {"companyOwnershipRequisitesField", "comanyNameField",
                "companyShortNameField", "companyGroupLookupPickerField",
                "companyDirectorField", "cityOfCompanyField",
                "regionOfCompanyField", "countryOfCompanyField",
                "addressOfCompanyField",
                "companyDescritionRichTextArea", "companyWorkingConditionsRichTextArea"};
        for (String id : controls) {
            int idx = xml.indexOf("id=\"" + id + "\"");
            assertTrue(id + ": поле не найдено", idx >= 0);
            int stylenameIdx = xml.indexOf("stylename=\"edit-form-control\"", idx);
            assertTrue(id + ": нет edit-form-control рядом с полем",
                    stylenameIdx > idx && stylenameIdx - idx < 600);
        }

        // Captions полей сохранены
        assertTrue(xml.contains("caption=\"Форма собственности\""));
        assertTrue(xml.contains("caption=\"msg://msgCompanyName\""));
        assertTrue(xml.contains("caption=\"mainMsg://msgCountryShortName\""));
        assertTrue(xml.contains("caption=\"msg://msgCompanyGroup\""));
        assertTrue(xml.contains("caption=\"msg://msgDirector\""));
        assertTrue(xml.contains("caption=\"msg://msgCityOfCompany\""));
        assertTrue(xml.contains("caption=\"msg://msgRegionOfCompany\""));
        assertTrue(xml.contains("caption=\"msg://msgCountryOfCompany\""));
        assertTrue(xml.contains("caption=\"msg://mshCompanyAddress\""));
        assertTrue(xml.contains("caption=\"msg://msgCompanyWorkCondition\""));
    }

    @Test
    public void dataBindingsViewsLoadersAndActionsPreserved() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Контейнеры и views.
        assertTrue(xml.contains("id=\"companyDc\""));
        assertTrue(xml.contains("view=\"company-edit-view\""));
        assertTrue(xml.contains("id=\"departmentOfCompanyDc\""));
        assertTrue(xml.contains("id=\"companyOwnershipsDc\""));
        assertTrue(xml.contains("id=\"companyDirectorsDc\""));
        assertTrue(xml.contains("view=\"person-picker-view\""));
        assertTrue(xml.contains("id=\"companyGroupDc\""));
        assertTrue(xml.contains("view=\"companyGroup-picker-view\""));
        assertTrue(xml.contains("id=\"cityOfCompaniesDc\""));
        assertTrue(xml.contains("view=\"city-location-view\""));
        assertTrue(xml.contains("id=\"regionOfCompaniesDc\""));
        assertTrue(xml.contains("view=\"region-browse-view\""));
        assertTrue(xml.contains("id=\"countryOfCompaniesDc\""));
        assertTrue(xml.contains("view=\"country-picker-view\""));

        // JPQL-запросы не изменились.
        assertTrue(xml.contains("select e from hunttech_Ownershup e"));
        assertTrue(xml.contains("select e from hunttech_Person e"));
        assertTrue(xml.contains("select e from hunttech_CompanyGroup e order by e.companyRuGroupName"));
        assertTrue(xml.contains("select e from hunttech_City e"));
        assertTrue(xml.contains("select e from hunttech_Region e"));
        assertTrue(xml.contains("select e from hunttech_Country e"));

        // Property bindings полей.
        assertTrue(xml.contains("property=\"ourLegalEntity\""));
        assertTrue(xml.contains("property=\"ourClient\""));
        assertTrue(xml.contains("property=\"companyOwnership\""));
        assertTrue(xml.contains("property=\"comanyName\""));
        assertTrue(xml.contains("property=\"companyShortName\""));
        assertTrue(xml.contains("property=\"companyGroup\""));
        assertTrue(xml.contains("property=\"companyDirector\""));
        assertTrue(xml.contains("property=\"cityOfCompany\""));
        assertTrue(xml.contains("property=\"regionOfCompany\""));
        assertTrue(xml.contains("property=\"countryOfCompany\""));
        assertTrue(xml.contains("property=\"addressOfCompany\""));
        assertTrue(xml.contains("property=\"companyDescription\""));
        assertTrue(xml.contains("property=\"workingConditions\""));
        assertTrue(xml.contains("property=\"fileCompanyLogo\""));

        // Логотип: единый OvaFallbackImage по эталону ProjectEdit/JobCandidateEdit
        // (176×176, ovalWidth/ovalHeight, fallback no-company.png, SCALE_DOWN) +
        // загрузчик в sidebar; legacy-пара image + default удалены.
        assertTrue("Нет OvaFallbackImage логотипа",
                xml.contains("<ovaFallbackImage id=\"companyLogoFileImage\""));
        assertTrue("OvaFallbackImage не 176×176 (width/height/ovalWidth/ovalHeight)",
                xml.contains("width=\"176px\"") && xml.contains("height=\"176px\"")
                        && xml.contains("ovalWidth=\"176px\"") && xml.contains("ovalHeight=\"176px\""));
        assertTrue("Нет fallbackThemePath icons/no-company.png",
                xml.contains("fallbackThemePath=\"icons/no-company.png\""));
        assertTrue("scaleMode не SCALE_DOWN (эталон IteractionListEdit)",
                xml.contains("scaleMode=\"SCALE_DOWN\""));
        assertFalse("Дублирующий fallback-image companyDefaultLogoFileImage остался",
                xml.contains("companyDefaultLogoFileImage"));
        assertTrue(xml.contains("id=\"companyLogoFileUpload\""));
        assertTrue(xml.contains("fileStoragePutMode=\"IMMEDIATE\""));
        assertTrue(xml.contains("showClearButton=\"true\""));
        assertTrue("dropZone не указывает на visual-блок sidebar",
                xml.contains("dropZone=\"companyEditorSidebarVisual\""));
        assertTrue(xml.contains("id=\"companyLogoPicBox\""));

        // Actions и ленивые вкладки (имена — контракт контроллера).
        assertTrue(xml.contains("type=\"picker_lookup\""));
        assertTrue(xml.contains("type=\"create\""));
        assertTrue(xml.contains("type=\"edit\""));
        assertTrue(xml.contains("type=\"remove\""));
        assertTrue(xml.contains("action=\"windowCommitAndClose\""));
        assertTrue(xml.contains("action=\"windowClose\""));
        assertTrue(xml.contains("id=\"mainTab\""));
        assertTrue(xml.contains("id=\"tabConpanyDetails\""));
        assertTrue(xml.contains("id=\"companyDescriptionTab\""));
        assertTrue(xml.contains("id=\"tabCompanyDepartament\""));

        // Навигационные кнопки sidebar = 3 вкладки TabSheet (эталон ProjectEdit),
        // focusComponent окна сохранён.
        assertTrue(xml.contains("id=\"companyEditorNavMain\""));
        assertTrue(xml.contains("id=\"companyEditorNavDescription\""));
        assertTrue(xml.contains("id=\"companyEditorNavDepartments\""));
        assertTrue(xml.contains("caption=\"mainMsg://msgCompanyDetail\""));
        assertTrue(xml.contains("caption=\"msg://msgCompanyDescription\""));
        assertTrue(xml.contains("caption=\"mainMsg://msgCompanyDepartament\""));
        assertTrue(xml.contains("focusComponent=\"comanyNameField\""));

        // Java: клик по пункту навигации переключает вкладку TabSheet,
        // активный пункт синхронизируется по SelectedTabChange.
        String java = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/company/CompanyEdit.java");
        assertTrue(java.contains("mainTab.setSelectedTab(\"tabConpanyDetails\")"));
        assertTrue(java.contains("mainTab.setSelectedTab(\"companyRequisitesTab\")"));
        assertTrue(java.contains("mainTab.setSelectedTab(\"companyDescriptionTab\")"));
        assertTrue(java.contains("mainTab.setSelectedTab(\"tabCompanyDepartament\")"));
        assertTrue(java.contains("TAB_TO_NAV_BUTTON"));
        // label-навигация видима на всех вкладках формы компании.
        assertTrue("Нет константы TABS_WITH_SIDEBAR_NAVIGATION",
                java.contains("TABS_WITH_SIDEBAR_NAVIGATION"));
        assertTrue("Контейнер навигации не инжектится",
                java.contains("VBoxLayout companyEditorSidebarNavigation"));
        assertTrue("setVisible по TABS_WITH_SIDEBAR_NAVIGATION отсутствует",
                java.contains("companyEditorSidebarNavigation.setVisible(")
                        && java.contains("TABS_WITH_SIDEBAR_NAVIGATION.contains"));
        assertTrue("tabConpanyDetails не в списке вкладок с навигацией",
                java.contains("\"tabConpanyDetails\""));
        assertTrue("companyRequisitesTab не в списке вкладок с навигацией",
                java.contains("\"companyRequisitesTab\""));
        assertTrue("companyDescriptionTab не в списке вкладок с навигацией",
                java.contains("\"companyDescriptionTab\""));
        assertTrue("tabCompanyDepartament не в списке вкладок с навигацией",
                java.contains("\"tabCompanyDepartament\""));
        assertTrue("Нет контейнера companyEditorSidebarNavigation в XML",
                xml.contains("id=\"companyEditorSidebarNavigation\""));
        assertTrue("логотип должен инжектиться как WebOvaFallbackImage (авто-fallback)",
                java.contains("WebOvaFallbackImage companyLogoFileImage"));
        assertFalse("legacy-переключатель пары image остался",
                java.contains("setCompanyPicImage"));
    }

    @Test
    public void everyThemeAppliesCompanyLocalScss() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/company-editor.scss");
        assertTrue("Канон SCSS пуст или не содержит mixin",
                canon.contains("@mixin company-editor-theme"));
        assertTrue("Нет фирменного тёмного фона #172638", canon.contains("#172638"));
        assertTrue("Нет канонического active #ffb11b", canon.contains("#ffb11b"));
        // Sidebar 270px с внутренними отступами 14px 16px 12px (контракт §4.2).
        assertTrue("Нет ширины sidebar 270px", canon.contains("width: 270px !important"));
        assertTrue("Нет внутренних отступов sidebar 14px 16px 12px",
                canon.contains("padding: 14px 16px 12px !important"));
        // Название (title) жёлтое #ffb11b 18px по центру — эталон.
        assertTrue("title не жёлтый 18px", canon.contains("color: #ffb11b !important")
                && canon.contains("font-size: 18px !important"));
        // Подпись типа записи (subtitle) не используется; sidebar скроллится
        // тонким скроллбаром (эталон OpenPositionEdit §4.2).
        assertFalse("subtitle-блок остался в SCSS",
                canon.contains(".edit-sidebar-subtitle"));
        assertTrue("нет тонкого скроллбара sidebar (scrollbar-width: thin)",
                canon.contains("scrollbar-width: thin"));
        assertTrue("Нет канонического hover rgba(255,255,255,0.08)",
                canon.contains("rgba(255, 255, 255, 0.08)"));
        assertTrue("Нет канонического активного фона rgba(255,177,27,0.12)",
                canon.contains("rgba(255, 177, 27, 0.12)"));
        // Пункты навигации: min-height 27px, высота по контенту (эталон
        // IteractionListEdit — длинная подпись переносится, строка не режется).
        assertTrue("nav-пункты без min-height 27px", canon.contains("min-height: 27px !important"));
        assertTrue("nav-пункты не по высоте контента (height: auto)",
                canon.contains("height: auto !important"));
        assertFalse("nav-пункты с фиксированной высотой (обрезает перенос текста)",
                canon.contains("max-height: 27px !important"));
        // Локальное wrap-правило с принудительной высотой запрещено (контракт
        // §3.1 — сдвигает текст относительно маркера): shared flex+align-items.
        assertFalse("локальное .v-button-wrap правило осталось (контракт §3.1)",
                canon.contains(".v-button-label-nav-item .v-button-wrap"));
        // halo-тема добавляет кнопке :before — отключаем.
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
        assertTrue("нет primary-кнопки", canon.contains(".company-editor-primary-action"));
        assertTrue("нет secondary-кнопки", canon.contains(".company-editor-secondary-action"));
        // Полоса-заголовок навигации «Разделы» (контракт §4.1): две inset-линии.
        assertTrue("Нет правила полосы-заголовка .company-editor-navigation-title",
                canon.contains(".company-editor-navigation-title"));
        assertTrue("Нет inset-линий полосы-заголовка (box-shadow)",
                canon.contains("rgba(255, 255, 255, 1) 0 1px 0 0 inset"));
        assertTrue("Нет разделителя полосы-заголовка (border-bottom)",
                canon.contains("border-bottom: 1px solid rgba(255, 255, 255, 0.14)"));
        // Визуальный блок sidebar: круглый логотип без рамки/тени.
        assertTrue("Нет блока .edit-sidebar-visual", canon.contains(".edit-sidebar-visual"));
        assertTrue("Нет круглой геометрии .company-editor-logo-image",
                canon.contains(".company-editor-logo-image"));
        assertTrue("Нет border-radius 50% у логотипа",
                canon.contains("border-radius: 50% !important"));
        // Раздел «Коротко» (ProjectEdit) в CompanyEdit отсутствует.
        assertFalse("блок «Коротко» не должен переезжать в company-editor.scss",
                canon.contains("short-description"));
        // Вкладки TabSheet оформляются ОБЩИМИ стилями тем; локальный дубль не нужен.
        assertFalse("вкладки не должны дублироваться в company-editor.scss",
                canon.contains(".edit-tabs .v-tabsheet-tabitemcell"));

        for (String theme : THEMES) {
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            assertTrue(theme + ": styles.scss не импортирует company-editor",
                    styles.contains("company-editor"));
            assertTrue(theme + ": styles.scss не вызывает @include company-editor-theme",
                    styles.contains("@include company-editor-theme;"));

            String local = readProjectFile(
                    "modules/web/themes/" + theme + "/com.company.hunttech/company-editor.scss");
            assertTrue("company-editor.scss не идентичен в теме " + theme, canon.equals(local));
        }
    }

    @Test
    public void workspaceFieldRowsRemainResponsiveWithoutChangingSidebar() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/company-editor.scss");

        assertFalse("вкладка company-main-tab не должна наследовать высоту поля ввода",
                canon.contains(".company-main-tab,"));
        assertTrue("hbox-строка workspace не переведена в flex-flow",
                canon.contains(".edit-workspace-content .v-horizontallayout > .v-expand")
                        && canon.contains("display: flex !important")
                        && canon.contains("flex-wrap: wrap !important"));
        assertTrue("слоты hbox не возвращены в normal flow",
                canon.contains(".edit-workspace-content .v-horizontallayout > .v-expand > .v-slot")
                        && canon.contains("position: static !important")
                        && canon.contains("left: auto !important")
                        && canon.contains("flex: 1 1 240px !important"));
        assertTrue("адаптивная правка не должна менять sidebar 270px",
                canon.contains("width: 270px !important")
                        && canon.contains("padding: 14px 16px 12px !important"));
    }

    @Test
    public void uploadButtonsFollowCanonicalDarkSidebarStyle() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/company-editor.scss");

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
                "modules/web/themes/hover/com.company.hunttech/company-editor.scss");

        // Sidebar отделён правой границей и тенью (эталон iteraction-list-sidebar).
        assertTrue("Нет border-right sidebar",
                canon.contains("border-right: 1px solid rgba(15, 23, 42, 0.78)"));
        assertTrue("Нет тени sidebar 5px 0 20px",
                canon.contains("box-shadow: 5px 0 20px rgba(15, 23, 42, 0.18)"));
        // Визуальный блок sidebar: min-height 104px (эталон identity-images).
        assertTrue("visual-блок не 104px", canon.contains("min-height: 104px"));
        // Spacer sidebar на всю высоту (атрибуты многострочные — по отдельности).
        assertTrue("companySidebarSpacer отсутствует",
                xml.contains("id=\"companySidebarSpacer\""));
        assertTrue("companySidebarSpacer без stylename edit-sidebar-spacer",
                xml.contains("id=\"companySidebarSpacer\"")
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
        assertTrue("Группа кнопок без MIDDLE_RIGHT",
                xml.contains("align=\"MIDDLE_RIGHT\""));
        assertTrue("Группа кнопок без межкнопочного зазора spacing=true",
                xml.contains("id=\"bottomActionsGroup\"")
                        && xml.contains("spacing=\"true\""));
        // RichTextArea и dataGrid не выталкивают ширину за границы вкладки.
        assertTrue("Нет ограничения .v-richtextarea", canon.contains(".v-richtextarea"));
        assertTrue("Нет ограничения .v-grid", canon.contains(".v-grid"));
    }

    private static String readProjectFile(String relativePath) throws IOException {
        // Относительные пути резолвятся от корня репозитория (gradle test для
        // :app-core работает с cwd=modules/core, а файлы лежат в корне проекта).
        return new String(
                Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8);
    }

    private static Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertTrue("Не найден корень проекта HRM HuntTech", root != null);
        return root;
    }
}
