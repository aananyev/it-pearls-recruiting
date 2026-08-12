package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает presentation-контракт Edit-формы «Дерево компетенций» HRM HuntTech
 * (SkillTreeEdit): sidebar 312px (эталон IteractionListEdit), общие edit-* и
 * label-* stylename, единый стиль полей edit-form-control и сохранённые
 * data bindings/loaders/actions. Бизнес-логика и loaders не проверяются.
 */
public class SkillTreeEditLayoutContractTest {

    private static final String SCREEN =
            "modules/web/src/com/company/hunttech/web/screens/skilltree/skill-tree-edit.xml";
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
        assertTrue(xml.contains("width=\"312px\""));
        assertTrue(xml.contains("stylename=\"edit-screen-layout\""));
        assertTrue(xml.contains("stylename=\"edit-workspace\""));
        assertTrue(xml.contains("stylename=\"label-navigation\""));
        assertTrue(xml.contains("label-nav-title"));
        assertTrue(xml.contains("label-nav-item label-nav-item-active"));
        assertTrue(xml.contains("stylename=\"edit-footer-actions\""));
        assertTrue(xml.contains("stylename=\"edit-toolbar\""));
        // Порядок идентификации как эталон IteractionListEdit: название (title)
        // СВЕРХУ, подпись типа (subtitle) СНИЗУ.
        int titleIdx = xml.indexOf("id=\"skillTreeSidebarTitle\"");
        int subtitleIdx = xml.indexOf("stylename=\"edit-sidebar-subtitle\"");
        assertTrue("нет title в identity", titleIdx >= 0);
        assertTrue("нет subtitle в identity", subtitleIdx >= 0);
        assertTrue("порядок не эталонный: subtitle раньше title",
                titleIdx < subtitleIdx);
        // Нижние действия: primary «Сохранить и закрыть» и secondary «Отмена»
        // (эталон IteractionListEdit).
        assertTrue(xml.contains("stylename=\"skill-tree-primary-action\""));
        assertTrue(xml.contains("stylename=\"skill-tree-secondary-action\""));
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
        assertTrue("нет полосы-заголовка skill-tree-navigation-title",
                xml.contains("label-nav-title skill-tree-navigation-title"));
    }

    @Test
    public void everyInputFieldUsesEditFormControl() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Каждый типовой input несёт общий stylename контракта (атрибуты XML
        // многострочные — проверяем id и stylename по отдельности, в пределах
        // 600 символов от открывающего тега поля).
        assertTrue(xml.contains("id=\"skillNameField\""));
        assertTrue(xml.contains("id=\"skillPriorityField\""));
        assertTrue(xml.contains("id=\"skillTreeField\""));
        assertTrue(xml.contains("id=\"specialisationField\""));
        assertTrue(xml.contains("id=\"wikiPateField\""));
        assertTrue(xml.contains("id=\"styleHighlightingField\""));

        String[] controls = {"skillNameField", "skillPriorityField",
                "wikiPateField", "styleHighlightingField"};
        for (String id : controls) {
            int idx = xml.indexOf("id=\"" + id + "\"");
            assertTrue(id + ": поле не найдено", idx >= 0);
            int stylenameIdx = xml.indexOf("stylename=\"edit-form-control\"", idx);
            // стиль поля лежит в пределах 600 символов после открывающего тега поля
            assertTrue(id + ": нет edit-form-control рядом с полем",
                    stylenameIdx > idx && stylenameIdx - idx < 600);
        }

        // Пейкеры (lookupPickerField) сознательно БЕЗ edit-form-control: общий
        // shared-стиль .edit-form-control .c-pickerfield {width:100%!important}
        // схлопывает XML-ширину 50% до 25%; ширины пейкеров задаются локальными
        // правилами канона (.edit-card .v-slot-c-combobox-pickerfield 50% /
        // .c-pickerfield 100%) — эталон IteractionListEdit, две колонки по 50%.
        String[] pickers = {"skillTreeField", "specialisationField"};
        for (String id : pickers) {
            int idx = xml.indexOf("id=\"" + id + "\"");
            assertTrue(id + ": пейкер не найден", idx >= 0);
            assertTrue(id + ": у пейкера не должно быть edit-form-control",
                    xml.indexOf("stylename=\"edit-form-control\"", idx) < 0
                            || xml.indexOf("stylename=\"edit-form-control\"", idx) - idx > 600);
            assertTrue(id + ": нет width=\"50%\" у пейкера",
                    xml.indexOf("width=\"50%\"", idx) > idx
                            && xml.indexOf("width=\"50%\"", idx) - idx < 600);
        }

        // Captions полей сохранены (могут быть на отдельных строках от id).
        assertTrue(xml.contains("caption=\"mainMsg://msgSkill\""));
        assertTrue(xml.contains("caption=\"msg://msgPrioritySkill\""));
        assertTrue(xml.contains("caption=\"mainMsg://msgSkillTree\""));
        assertTrue(xml.contains("caption=\"msg://msgSpecialisation\""));
        assertTrue(xml.contains("caption=\"msg://msgWikiPage\""));
        assertTrue(xml.contains("caption=\"msg://msgStyleHighlighting\""));
    }

    @Test
    public void dataBindingsViewsLoadersAndActionsPreserved() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Контейнеры и views.
        assertTrue(xml.contains("id=\"skillTreeDc\""));
        assertTrue(xml.contains("view=\"skillTree-edit-view\""));
        assertTrue(xml.contains("id=\"skillTreesDc\""));
        assertTrue(xml.contains("view=\"skillTree-picker-view\""));
        assertTrue(xml.contains("id=\"skillTreesLc\" cacheable=\"true\""));
        assertTrue(xml.contains("id=\"specialisationDc\""));
        assertTrue(xml.contains("view=\"specialisation-picker-view\""));
        assertTrue(xml.contains("id=\"specialisationDl\" cacheable=\"true\""));

        // JPQL-запросы не изменились.
        assertTrue(xml.contains("select e from hunttech_SkillTree e"));
        assertTrue(xml.contains("where e.skillTree is null"));
        assertTrue(xml.contains("order by e.skillName"));
        assertTrue(xml.contains("select e from hunttech_Specialisation e order by e.specRuName"));

        // Property bindings полей.
        assertTrue(xml.contains("property=\"skillName\""));
        assertTrue(xml.contains("property=\"prioritySkill\""));
        assertTrue(xml.contains("property=\"notParsing\""));
        assertTrue(xml.contains("property=\"skillTree\""));
        assertTrue(xml.contains("property=\"specialisation\""));
        assertTrue(xml.contains("property=\"wikiPage\""));
        assertTrue(xml.contains("property=\"styleHighlighting\""));
        assertTrue(xml.contains("property=\"comment\""));
        // Логотип и upload привязаны к корневому контейнеру (атрибуты
        // многострочные — проверяем по отдельности).
        assertTrue(xml.contains("id=\"skillPic\""));
        assertTrue(xml.contains("id=\"fileImageSkillUpload\""));
        int picIdx = xml.indexOf("id=\"skillPic\"");
        int dcIdx = xml.indexOf("dataContainer=\"skillTreeDc\"", picIdx);
        assertTrue("skillPic: нет dataContainer skillTreeDc рядом",
                dcIdx > picIdx && dcIdx - picIdx < 300);
        assertTrue(xml.contains("property=\"fileImageLogo\""));
        assertTrue(xml.contains("fileStoragePutMode=\"IMMEDIATE\""));
        assertTrue(xml.contains("showClearButton=\"true\""));
        // Превью логотипа — круглый OvaFallbackImage по эталону JobCandidateEdit:
        // овальная геометрия 176px и fallback-картинка при отсутствии файла.
        assertTrue("skillPic не ovaFallbackImage (круглый аватар)",
                xml.contains("<ovaFallbackImage id=\"skillPic\""));
        assertTrue("нет ovalWidth/ovalHeight у skillPic",
                xml.contains("ovalWidth=\"176px\"") && xml.contains("ovalHeight=\"176px\""));
        assertTrue("нет fallback-картинки no-programmer.jpeg у skillPic",
                xml.contains("fallbackThemePath=\"icons/no-programmer.jpeg\""));
        assertTrue("нет локального класса круглого аватара skill-tree-logo-image",
                xml.contains("stylename=\"skill-tree-logo-image\""));

        // Actions и invoke.
        assertTrue(xml.contains("type=\"picker_lookup\""));
        assertTrue(xml.contains("invoke=\"parseWikiToDescription\""));
        assertTrue(xml.contains("invoke=\"focusMainSection\""));
        assertTrue(xml.contains("invoke=\"focusDescriptionSection\""));
        assertTrue(xml.contains("action=\"windowCommitAndClose\""));
        assertTrue(xml.contains("action=\"windowClose\""));

        // Навигационные кнопки sidebar.
        assertTrue(xml.contains("id=\"skillTreeMainNav\""));
        assertTrue(xml.contains("id=\"skillTreeDescriptionNav\""));
        // Визуальный блок sidebar и dropZone upload.
        assertTrue(xml.contains("id=\"skillTreeVisual\""));
        assertTrue(xml.contains("dropZone=\"skillTreeVisual\""));
    }

    @Test
    public void everyThemeAppliesSkillTreeLocalScss() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/skill-tree-editor.scss");
        assertTrue("Канон SCSS пуст или не содержит mixin", canon.contains("@mixin skill-tree-editor-theme"));
        assertTrue("Нет фирменного тёмного фона #172638", canon.contains("#172638"));
        assertTrue("Нет канонического active #ffb11b", canon.contains("#ffb11b"));
        // Sidebar 312px с внутренними отступами 14px 16px 12px — эталон IteractionListEdit.
        assertTrue("Нет ширины sidebar 312px", canon.contains("width: 312px !important"));
        assertTrue("Нет внутренних отступов sidebar 14px 16px 12px",
                canon.contains("padding: 14px 16px 12px !important"));
        // Визуальный блок sidebar прозрачный (без карточки-рамки), как identity-images эталона.
        assertTrue("visual-блок не прозрачный", canon.contains("background: transparent !important"));
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
        // Пункты навигации строго 27px (кнопки Vaadin без базовой высоты halo-темы).
        assertTrue("nav-кнопки не 27px", canon.contains("height: 27px !important"));
        assertTrue("nav-кнопки не центрированы", canon.contains("align-items: center !important"));
        // Заголовок toolbar 20px и описание 18px — эталон.
        assertTrue("toolbar title не 20px", canon.contains("font-size: 20px !important"));
        // Нижняя панель: отступы 11px 20px, кнопки 14px/600 высотой 40px, primary/secondary.
        assertTrue("footer не 11px 20px", canon.contains("padding: 11px 20px !important"));
        assertTrue("нет primary-кнопки", canon.contains(".skill-tree-primary-action"));
        assertTrue("нет secondary-кнопки", canon.contains(".skill-tree-secondary-action"));
        // Полоса-заголовок навигации «Разделы» (контракт §4.1): две inset-линии.
        assertTrue("Нет правила полосы-заголовка .skill-tree-navigation-title",
                canon.contains(".skill-tree-navigation-title"));
        assertTrue("Нет inset-линий полосы-заголовка (box-shadow)",
                canon.contains("rgba(255, 255, 255, 1) 0 1px 0 0 inset"));
        assertTrue("Нет разделителя полосы-заголовка (border-bottom)",
                canon.contains("border-bottom: 1px solid rgba(255, 255, 255, 0.14)"));
        // Визуальный блок sidebar: круглый аватар 96px по эталону OvaFallbackImage.
        assertTrue("Нет блока .edit-sidebar-visual", canon.contains(".edit-sidebar-visual"));
        assertTrue("Нет круглой геометрии .skill-tree-logo-image",
                canon.contains(".skill-tree-logo-image"));
        assertTrue("Нет border-radius 50% у аватара",
                canon.contains("border-radius: 50% !important"));
        assertTrue("Нет рамки аватара (border 3px white)",
                canon.contains("border: 3px solid rgba(255, 255, 255, 0.90)"));
        // Правые карточки по эталону.
        assertTrue("Нет карточек .edit-card с радиусом 8px", canon.contains("border-radius: 8px"));
        assertTrue("Нет полей 38px (.edit-card .v-textfield)",
                canon.contains(".edit-card .v-textfield"));

        for (String theme : THEMES) {
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            assertTrue(theme + ": styles.scss не импортирует skill-tree-editor",
                    styles.contains("skill-tree-editor"));
            assertTrue(theme + ": styles.scss не вызывает @include skill-tree-editor-theme",
                    styles.contains("@include skill-tree-editor-theme;"));

            String local = readProjectFile(
                    "modules/web/themes/" + theme + "/com.company.hunttech/skill-tree-editor.scss");
            assertTrue("skill-tree-editor.scss не идентичен в теме " + theme, canon.equals(local));
        }
    }

    @Test
    public void uploadButtonsFollowCanonicalDarkSidebarStyle() throws IOException {
        String xml = readProjectFile(SCREEN);
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/skill-tree-editor.scss");

        // Кнопки «Загрузить»/«Очистить» загрузчика логотипа имеют канонический вид
        // кнопок upload в тёмном sidebar (эталон JobCandidateEdit): пара 96×36,
        // полупрозрачный белый фон, центрирование в блоке визуала.
        assertTrue("Загрузчик логотипа отсутствует в XML",
                xml.contains("id=\"fileImageSkillUpload\""));
        assertTrue("Нет растяжения .c-fileupload-wrapper на ширину блока",
                canon.contains(".edit-sidebar-visual .c-fileupload-wrapper"));
        assertTrue("Wrapper не block", canon.contains("display: block !important"));
        // Контейнер пары: flex-центрирование с зазором 10px по ширине визуального блока.
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
        // Отдельные правила для «Очистить» и скрытого file input загрузчика.
        assertTrue("Нет правила .c-fileupload-clear",
                canon.contains(".edit-sidebar-visual .c-fileupload-clear"));
        assertTrue("Нет геометрии file input", canon.contains(".edit-sidebar-visual .c-fileupload input"));
        // Hover-состояние пары (светлее фон и рамка).
        assertTrue("Нет hover-фона кнопок", canon.contains("rgba(255, 255, 255, 0.14) !important"));
        assertTrue("Нет hover-рамки кнопок", canon.contains("rgba(255, 255, 255, 0.58) !important"));
    }

    @Test
    public void visualContractFollowsIteractionListEditReference() throws IOException {
        String xml = readProjectFile(SCREEN);
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/skill-tree-editor.scss");

        // Контент рабочей области: общий edit-workspace-content + локальный класс
        // с отступами финального слоя эталона (8px 20px 24px).
        assertTrue("skillTreeSections без edit-workspace-content",
                xml.contains("stylename=\"edit-workspace-content skill-tree-content\""));
        assertTrue("Нет отступов контента 8px 20px 24px",
                canon.contains("padding: 8px 20px 24px !important"));
        // Spacer sidebar на всю высоту (эталон iteractionListSidebarSpacer 100%×100%).
        assertTrue("skillTreeSidebarSpacer без height=100%",
                xml.contains("id=\"skillTreeSidebarSpacer\" width=\"100%\" height=\"100%\""));
        // Sidebar отделён правой границей и тенью (эталон iteraction-list-sidebar).
        assertTrue("Нет border-right sidebar",
                canon.contains("border-right: 1px solid rgba(15, 23, 42, 0.78)"));
        assertTrue("Нет тени sidebar 5px 0 20px",
                canon.contains("box-shadow: 5px 0 20px rgba(15, 23, 42, 0.18)"));
        // Визуальный блок sidebar: min-height 104px без нижнего отступа (эталон identity-images).
        assertTrue("visual-блок не 104px", canon.contains("min-height: 104px"));
        assertTrue("visual-блок имеет нижний отступ",
                !canon.contains("padding: 0 0 6px !important"));
        // Заголовок карточки: 16px/23px, min-height 46px, padding 11px 16px,
        // без вало-inset-shadow — финальный слой эталона (flat-section-title).
        assertTrue("Заголовок карточки не 16px", canon.contains("font-size: 16px !important"));
        assertTrue("Заголовок карточки не 23px", canon.contains("line-height: 23px !important"));
        assertTrue("Заголовок карточки не min-height 46px", canon.contains("min-height: 46px"));
        assertTrue("Заголовок карточки не padding 11px 16px",
                canon.contains("padding: 11px 16px"));
        assertTrue("Заголовок карточки сохранил вало box-shadow",
                canon.contains("box-shadow: none !important"));
        // Контент карточки: отступы 14px 16px 16px (эталон flat-section-body).
        assertTrue("Нет отступов контента карточки 14px 16px 16px",
                canon.contains("padding: 14px 16px 16px !important"));
        // Межкарточный отступ 12px (эталон flat-section margin-bottom 12px).
        assertTrue("margin карточек не 12px", canon.contains("margin-bottom: 12px"));
        // Чекбокс карточки: 14px/1.4 mix 78% (эталон flat-section .v-checkbox label).
        assertTrue("Нет стиля чекбокса 14px/1.4",
                canon.contains(".edit-card .v-checkbox label"));
        assertTrue("Чекбокс не 14px", canon.contains("font-size: 14px !important"));
        // RichTextArea — единый стиль полей (рамка rgba 20%, скругление 5px).
        assertTrue("Нет стиля .v-richtextarea", canon.contains(".edit-card .v-richtextarea"));
        // Footer: верхняя тень и hover-эффект кнопок (эталон iteraction-list-footer).
        assertTrue("Нет верхней тени footer",
                canon.contains("box-shadow: 0 -2px 8px rgba(15, 23, 42, 0.04)"));
        assertTrue("Нет hover footer-кнопок", canon.contains("filter: brightness(0.98)"));
        // Footer XML: expand-спейсер + группа AUTO/MIDDLE_RIGHT (эталон editActionsGroup).
        assertTrue("editActions без expand-спейсера",
                xml.contains("expand=\"skillTreeActionsSpacer\""));
        assertTrue("Нет группы skillTreeActionsGroup AUTO/MIDDLE_RIGHT",
                xml.contains("id=\"skillTreeActionsGroup\" width=\"AUTO\""));
        assertTrue("Спейсер footer без height=1px",
                xml.contains("id=\"skillTreeActionsSpacer\" width=\"100%\" height=\"1px\""));
        // Ряд 4: чекбокс слева + spacer + кнопка «Загрузить описание» справа —
        // вынос checkbox и кнопки из рядов 1/3 освобождает expand-поля «Навык» и «Wiki».
        assertTrue("Нет ряда 4 skillTreeMainRow4",
                xml.contains("id=\"skillTreeMainRow4\""));
        assertTrue("Ряд 4 без expand-спейсера",
                xml.contains("expand=\"skillTreeMainRow4Spacer\""));
        assertTrue("notParsingCheckBox не в ряду 4",
                xml.indexOf("notParsingCheckBox") > xml.indexOf("skillTreeMainRow4"));
        assertTrue("parseWikiText не после spacer ряда 4",
                xml.indexOf("parseWikiText") > xml.indexOf("skillTreeMainRow4Spacer"));
        // Фиксированные ширины полей (220px/50%/240px) не схлопываются shared-стилем:
        // локальная отмена width:100%!important для filterselect/picker карточек.
        assertTrue("Нет отмены width для filterselect карточек",
                canon.contains(".edit-card .v-filterselect"));
        assertTrue("Нет width: auto !important",
                canon.contains("width: auto !important"));
        // Мёртвые классы удалены (в XML нет summary-блока и grid).
        assertTrue("Мёртвый .edit-sidebar-summary остался в SCSS",
                !canon.contains(".edit-sidebar-summary"));
        assertTrue("Мёртвый .v-gridlayout остался в SCSS карточек",
                !canon.contains(".edit-card .v-gridlayout"));
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
