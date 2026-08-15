package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Защищает presentation-контракт Edit-формы «Департамент» HRM HuntTech
 * (CompanyDepartamentEdit): sidebar 270px (контракт §4.2), общие edit-* и
 * label-* stylename, единый стиль полей edit-form-control, полоса-заголовок
 * навигации «Разделы» (пункты = вкладки TabSheet, эталон ProjectEdit),
 * статичная иллюстрация ovalImage 176×176 и сохранённые data bindings/loaders/
 * actions. Бизнес-логика и loaders не проверяются.
 */
public class CompanyDepartamentEditLayoutContractTest {

    private static final String SCREEN =
            "modules/web/src/com/company/hunttech/web/screens/companydepartament/company-departament-edit.xml";
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
        int titleIdx = xml.indexOf("id=\"companyDepartamentSidebarTitle\"");
        assertTrue("нет title в identity", titleIdx >= 0);
        assertFalse("подпись типа записи (subtitle) осталась в identity",
                xml.contains("stylename=\"edit-sidebar-subtitle\""));
        // Нижние действия: primary «Сохранить и закрыть» и secondary «Отмена».
        assertTrue(xml.contains("stylename=\"company-departament-editor-primary-action\""));
        assertTrue(xml.contains("stylename=\"company-departament-editor-secondary-action\""));
        // legacy-форма focusComponent="form" удалена (контрактная форма с полями).
        assertFalse("legacy-форма id=\"form\" осталась в layout",
                xml.contains("<form id=\"form\""));
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
        assertTrue("нет полосы-заголовка company-departament-editor-navigation-title",
                xml.contains("label-nav-title company-departament-editor-navigation-title"));
        // Четыре контрактных карточки формы.
        assertTrue(xml.contains("id=\"companyDepartamentMainCard\""));
        assertTrue(xml.contains("id=\"companyDepartamentDescriptionCard\""));
        assertTrue(xml.contains("id=\"companyDepartamentProjectsCard\""));
        assertTrue(xml.contains("id=\"companyDepartamentTemplateCard\""));
        // Прокручиваемый контент первой вкладки — контрактные классы.
        assertTrue(xml.contains("stylename=\"edit-workspace edit-workspace-scroll\""));
        assertTrue(xml.contains("stylename=\"edit-workspace-content\""));
    }

    @Test
    public void everyInputFieldUsesEditFormControl() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Каждый типовой input несёт общий stylename контракта (атрибуты XML
        // многострочные — проверяем id и stylename по отдельности, в пределах
        // 600 символов от открывающего тега поля).
        String[] controls = {"departamentRuNameField", "companyNameField",
                "departamentHrDirectorField", "departamentDirectorField",
                "departamentNumberOfProgrammersField", "departamentDescriptionField",
                "templateLetterRichTextArea"};
        for (String id : controls) {
            int idx = xml.indexOf("id=\"" + id + "\"");
            assertTrue(id + ": поле не найдено", idx >= 0);
            int stylenameIdx = xml.indexOf("stylename=\"edit-form-control\"", idx);
            assertTrue(id + ": нет edit-form-control рядом с полем",
                    stylenameIdx > idx && stylenameIdx - idx < 600);
        }

        // Captions полей сохранены (1:1 со старым дескриптором).
        assertTrue(xml.contains("caption=\"msg://msgDepartamentName\""));
        assertTrue(xml.contains("caption=\"msg://msgCompanyName\""));
        assertTrue(xml.contains("caption=\"msg://msgHrDirectorOfDepartament\""));
        assertTrue(xml.contains("caption=\"msg://msdDirectorName\""));
        assertTrue(xml.contains("caption=\"msg://msgNumberOfPosition\""));
        assertTrue(xml.contains("caption=\"msg://msgShortDescription\""));
        // Поля растянуты на всю ширину карточки (legacy width=50% удалён).
        assertFalse("в форме остались поля width=50%", xml.contains("width=\"50%\""));
    }

    @Test
    public void dataBindingsViewsLoadersAndActionsPreserved() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Контейнеры и views.
        assertTrue(xml.contains("id=\"companyDepartamentDc\""));
        assertTrue(xml.contains("view=\"companyDepartament-edit-view\""));
        assertTrue(xml.contains("id=\"companyDepartamentProjectOfDepartmentsDc\""));
        assertTrue(xml.contains("property=\"projectOfDepartment\""));
        assertTrue(xml.contains("id=\"companyNamesDc\""));
        assertTrue(xml.contains("view=\"company-picker-view\""));
        assertTrue(xml.contains("id=\"departamentHrDirectorsDc\""));
        assertTrue(xml.contains("id=\"departamentDirectorsDc\""));
        assertTrue(xml.contains("view=\"person-picker-view\""));

        // JPQL-запросы не изменились.
        assertTrue(xml.contains("select e from hunttech_Company e"));
        assertTrue(xml.contains("order by e.comanyName"));
        assertTrue(xml.contains("select e from hunttech_Person e order by e.secondName, e.firstName"));

        // Property bindings полей.
        assertTrue(xml.contains("property=\"departamentRuName\""));
        assertTrue(xml.contains("property=\"companyName\""));
        assertTrue(xml.contains("property=\"departamentHrDirector\""));
        assertTrue(xml.contains("property=\"departamentDirector\""));
        assertTrue(xml.contains("property=\"departamentNumberOfProgrammers\""));
        assertTrue(xml.contains("property=\"departamentDescription\""));
        assertTrue(xml.contains("property=\"templateLetter\""));

        // Вкладки (имена — контракт ленивой загрузки контроллера) и вложенные
        // коллекции/таблица сохранены 1:1.
        assertTrue(xml.contains("id=\"tabSheetDepartment\""));
        assertTrue(xml.contains("id=\"tabEditProject\""));
        assertTrue(xml.contains("id=\"tabOpenPosition\""));
        assertTrue(xml.contains("id=\"tabTemplateLetter\""));
        assertTrue(xml.contains("id=\"companyDepartamentTable\""));
        assertTrue(xml.contains("id=\"addButton\""));
        assertTrue(xml.contains("id=\"editButton\""));
        assertTrue(xml.contains("id=\"removeButton\""));
        assertTrue(xml.contains("type=\"add\""));
        assertTrue(xml.contains("type=\"edit\""));
        assertTrue(xml.contains("type=\"remove\""));
        assertTrue(xml.contains("column id=\"projectName\""));
        assertTrue(xml.contains("column id=\"startProjectDate\""));
        assertTrue(xml.contains("column id=\"endProjectDate\""));
        assertTrue(xml.contains("action=\"companyDepartamentTable.add\""));
        assertTrue(xml.contains("action=\"windowCommitAndClose\""));
        assertTrue(xml.contains("action=\"windowClose\""));

        // Навигационные кнопки sidebar = 3 вкладки TabSheet (эталон ProjectEdit),
        // focusComponent окна сохранён.
        assertTrue(xml.contains("id=\"companyDepartamentNavMain\""));
        assertTrue(xml.contains("id=\"companyDepartamentNavProjects\""));
        assertTrue(xml.contains("id=\"companyDepartamentNavTemplate\""));
        assertTrue(xml.contains("caption=\"msg://TabProject\""));
        assertTrue(xml.contains("caption=\"msg://TabOpenPosition\""));
        assertTrue(xml.contains("caption=\"msg://msgTemplateLetter\""));
        assertTrue(xml.contains("focusComponent=\"departamentRuNameField\""));

        // Java: клик по пункту навигации переключает вкладку TabSheet,
        // активный пункт синхронизируется по SelectedTabChange; lazy-load 1:1.
        String java = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/companydepartament/CompanyDepartamentEdit.java");
        assertTrue(java.contains("tabSheetDepartment.setSelectedTab(\"tabEditProject\")"));
        assertTrue(java.contains("tabSheetDepartment.setSelectedTab(\"tabOpenPosition\")"));
        assertTrue(java.contains("tabSheetDepartment.setSelectedTab(\"tabTemplateLetter\")"));
        assertTrue(java.contains("TAB_TO_NAV_BUTTON"));
        assertTrue(java.contains("loadDepartamentDescription"));
        assertTrue(java.contains("loadTemplateLetter"));
        assertTrue(java.contains("loadProjects"));
        assertTrue(java.contains("\"projectOfDepartment\", \"project-department-child-view\""));
        // Правило 3.6: label-навигация видима только на вкладках с 2+ блоками
        // (tabEditProject — 2 карточки; таблица проектов и шаблон письма —
        // одноблочные, контейнер скрывается целиком).
        assertTrue("Нет константы TABS_WITH_SIDEBAR_NAVIGATION",
                java.contains("TABS_WITH_SIDEBAR_NAVIGATION"));
        assertTrue("Контейнер навигации не инжектится",
                java.contains("VBoxLayout companyDepartamentEditorSidebarNavigation"));
        assertTrue("setVisible по TABS_WITH_SIDEBAR_NAVIGATION отсутствует",
                java.contains("companyDepartamentEditorSidebarNavigation.setVisible(")
                        && java.contains("TABS_WITH_SIDEBAR_NAVIGATION.contains"));
        assertTrue("tabEditProject не в списке вкладок с навигацией",
                java.contains("\"tabEditProject\""));
        // Контейнер навигации присутствует в XML (контракт инжекции).
        assertTrue("Нет контейнера companyDepartamentEditorSidebarNavigation в XML",
                xml.contains("id=\"companyDepartamentEditorSidebarNavigation\""));
    }

    @Test
    public void everyThemeAppliesCompanyDepartamentLocalScss() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/company-departament-editor.scss");
        assertTrue("Канон SCSS пуст или не содержит mixin",
                canon.contains("@mixin company-departament-editor-theme"));
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
        assertTrue("нет primary-кнопки", canon.contains(".company-departament-editor-primary-action"));
        assertTrue("нет secondary-кнопки", canon.contains(".company-departament-editor-secondary-action"));
        // Полоса-заголовок навигации «Разделы» (контракт §4.1): две inset-линии.
        assertTrue("Нет правила полосы-заголовка .company-departament-editor-navigation-title",
                canon.contains(".company-departament-editor-navigation-title"));
        assertTrue("Нет inset-линий полосы-заголовка (box-shadow)",
                canon.contains("rgba(255, 255, 255, 1) 0 1px 0 0 inset"));
        assertTrue("Нет разделителя полосы-заголовка (border-bottom)",
                canon.contains("border-bottom: 1px solid rgba(255, 255, 255, 0.14)"));
        // Визуальный блок sidebar: круглая иллюстрация без рамки/тени.
        assertTrue("Нет блока .edit-sidebar-visual", canon.contains(".edit-sidebar-visual"));
        assertTrue("Нет круглой геометрии .company-departament-editor-logo-image",
                canon.contains(".company-departament-editor-logo-image"));
        assertTrue("Нет border-radius 50% у иллюстрации",
                canon.contains("border-radius: 50% !important"));
        // Вкладки TabSheet оформляются ОБЩИМИ стилями тем; локальный дубль не нужен.
        assertFalse("вкладки не должны дублироваться в company-departament-editor.scss",
                canon.contains(".edit-tabs .v-tabsheet-tabitemcell"));
        // Форма использует table (не dataGrid): правило .v-grid не переносится.
        assertTrue("Нет ограничения .v-table (таблица проектов)",
                canon.contains(".v-table {"));
        assertFalse("правило .v-grid (dataGrid) не должно быть в этом partial",
                canon.contains(".v-grid"));

        for (String theme : THEMES) {
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            assertTrue(theme + ": styles.scss не импортирует company-departament-editor",
                    styles.contains("company-departament-editor"));
            assertTrue(theme + ": styles.scss не вызывает @include company-departament-editor-theme",
                    styles.contains("@include company-departament-editor-theme;"));

            String local = readProjectFile(
                    "modules/web/themes/" + theme + "/com.company.hunttech/company-departament-editor.scss");
            assertTrue("company-departament-editor.scss не идентичен в теме " + theme, canon.equals(local));
        }
    }

    @Test
    public void sidebarLogoIsStaticOvalImageInAllThemes() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Статичная круглая иллюстрация департамента 176×176 (канон серии
        // справочников) — без загрузки изображения и без fallback-компонента.
        assertTrue("Нет статичного ovalImage иллюстрации",
                xml.contains("<ovalImage id=\"companyDepartamentLogoImage\""));
        assertTrue("Иллюстрация не 176×176 (width/height/ovalWidth/ovalHeight)",
                xml.contains("width=\"176px\"") && xml.contains("height=\"176px\"")
                        && xml.contains("ovalWidth=\"176px\"") && xml.contains("ovalHeight=\"176px\""));
        assertTrue("scaleMode не SCALE_DOWN (эталон IteractionListEdit)",
                xml.contains("scaleMode=\"SCALE_DOWN\""));
        assertTrue("Нет theme path icons/dictionaries/company-departament.png",
                xml.contains("<theme path=\"icons/dictionaries/company-departament.png\"/>"));
        // Загрузка изображения формой не предусмотрена: upload/fallback отсутствуют.
        assertFalse("upload-загрузчик не должен быть в sidebar (нет загрузки логотипа)",
                xml.contains("<upload"));
        assertFalse("ovaFallbackImage не должен использоваться (нет файла-свойства)",
                xml.contains("<ovaFallbackImage"));

        // Иконка существует во всех 7 темах (каталог icons копируется в build).
        for (String theme : THEMES) {
            String png = readProjectFile(
                    "modules/web/themes/" + theme + "/icons/dictionaries/company-departament.png");
            assertTrue("icons/dictionaries/company-departament.png пуст в теме " + theme,
                    png.length() > 0);
        }
    }

    @Test
    public void visualContractFollowsIteractionListEditReference() throws IOException {
        String xml = readProjectFile(SCREEN);
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/company-departament-editor.scss");

        // Sidebar отделён правой границей и тенью (эталон iteraction-list-sidebar).
        assertTrue("Нет border-right sidebar",
                canon.contains("border-right: 1px solid rgba(15, 23, 42, 0.78)"));
        assertTrue("Нет тени sidebar 5px 0 20px",
                canon.contains("box-shadow: 5px 0 20px rgba(15, 23, 42, 0.18)"));
        // Визуальный блок sidebar: min-height 104px (эталон identity-images).
        assertTrue("visual-блок не 104px", canon.contains("min-height: 104px"));
        // Spacer sidebar на всю высоту (атрибуты многострочные — по отдельности).
        assertTrue("companyDepartamentSidebarSpacer отсутствует",
                xml.contains("id=\"companyDepartamentSidebarSpacer\""));
        assertTrue("companyDepartamentSidebarSpacer без stylename edit-sidebar-spacer",
                xml.contains("id=\"companyDepartamentSidebarSpacer\"")
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
        // RichTextArea и Vaadin Table не выталкивают ширину за границы вкладки.
        assertTrue("Нет ограничения .v-richtextarea", canon.contains(".v-richtextarea"));
        assertTrue("Нет ограничения .v-table", canon.contains(".v-table"));
        // Хинт sidebar сохранён.
        assertTrue("Нет hint sidebar",
                xml.contains("id=\"companyDepartamentSidebarHint\"")
                        && xml.contains("stylename=\"edit-sidebar-hint\""));
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
