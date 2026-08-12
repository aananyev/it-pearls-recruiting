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
 * Защищает presentation-контракт Edit-формы «Шаблоны промптов» HRM HuntTech
 * (VacancyPromptTemplateEdit): sidebar 270px, общие edit-* и label-* stylename,
 * полоса-заголовок навигации «Разделы» (контракт §4.1), единый стиль полей
 * edit-form-control и сохранённые data bindings/actions.
 * Бизнес-логика и loaders не проверяются.
 */
public class VacancyPromptTemplateEditContractTest {

    private static final String SCREEN =
            "modules/web/src/com/company/hunttech/web/screens/vacancyprompttemplate/";
    private static final String THEME_PARTIAL = "com.company.hunttech/vacancy-prompt-template-editor.scss";
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
    public void formUsesSharedSidebarAndWorkspaceOrder() throws IOException {
        String xml = readProjectFile(SCREEN + "vacancy-prompt-template-edit.xml");

        assertTrue(xml.contains("stylename=\"vacancy-prompt-template-editor\""));
        assertTrue(xml.contains("stylename=\"edit-sidebar\""));
        assertTrue(xml.contains("width=\"270px\""));
        assertTrue(xml.contains("stylename=\"edit-screen-layout\""));
        assertTrue(xml.contains("stylename=\"edit-workspace\""));
        assertTrue(xml.contains("stylename=\"label-navigation\""));
        assertTrue(xml.contains("label-nav-title"));
        assertTrue(xml.contains("label-nav-item label-nav-item-active"));
        assertTrue(xml.contains("stylename=\"edit-footer-actions\""));
        assertTrue(xml.contains("stylename=\"edit-toolbar\""));
        assertTrue(xml.contains("stylename=\"edit-toolbar-description\""));
    }

    @Test
    public void cardsArePanelsWithContractClasses() throws IOException {
        String xml = readProjectFile(SCREEN + "vacancy-prompt-template-edit.xml");

        assertTrue("legacy-класс edit-section-card остался", !xml.contains("edit-section-card"));
        assertTrue("legacy-класс edit-toolbar-subtitle остался", !xml.contains("edit-toolbar-subtitle"));
        assertTrue(xml.contains("stylename=\"edit-card\""));
        // Карточки groupBox рендерятся как Vaadin Panel (v-panel-caption), иначе
        // CUBA-рендер c-groupbox-caption не матчит SCSS-правила контракта.
        assertTrue("edit-card без showAsPanel (заголовок карточки не стилизуется)",
                xml.contains("showAsPanel=\"true\""));
        // Полоса-заголовок навигации «Разделы» (контракт §4.1): класс секции
        // поверх label-nav-title — две горизонтальные inset-линии.
        assertTrue("нет полосы-заголовка vacancy-prompt-template-navigation-title",
                xml.contains("label-nav-title vacancy-prompt-template-navigation-title"));
    }

    @Test
    public void everyInputFieldUsesEditFormControl() throws IOException {
        String xml = readProjectFile(SCREEN + "vacancy-prompt-template-edit.xml");

        // Атомарные проверки (атрибуты XML многострочные — по одному атрибуту).
        assertTrue(xml.contains("id=\"codeField\""));
        assertTrue(xml.contains("property=\"code\""));
        assertTrue(xml.contains("caption=\"msg://templateCodeCaption\""));
        assertTrue(xml.contains("id=\"nameField\""));
        assertTrue(xml.contains("property=\"name\""));
        assertTrue(xml.contains("caption=\"msg://templateName.caption\""));
        assertTrue(xml.contains("id=\"temperatureField\""));
        assertTrue(xml.contains("property=\"temperature\""));
        assertTrue(xml.contains("caption=\"msg://templateTemperatureCaption\""));
        assertTrue(xml.contains("id=\"systemContextField\""));
        assertTrue(xml.contains("caption=\"msg://aiRole.caption\""));
        assertTrue(xml.contains("id=\"promptTextField\""));
        assertTrue(xml.contains("caption=\"msg://mainTask.caption\""));
        assertTrue(xml.contains("stylename=\"edit-form-control\""));
    }

    @Test
    public void dataBindingsViewsAndFocusContractPreserved() throws IOException {
        String xml = readProjectFile(SCREEN + "vacancy-prompt-template-edit.xml");

        assertTrue(xml.contains("id=\"vacancyPromptTemplateDc\""));
        assertTrue(xml.contains("view=\"vacancyPromptTemplate-edit-view\""));
        assertTrue(xml.contains("focusComponent=\"codeField\""));
        assertTrue(xml.contains("invoke=\"focusMainSection\""));
        assertTrue(xml.contains("invoke=\"focusPromptSection\""));
        assertTrue(xml.contains("property=\"systemContext\""));
        assertTrue(xml.contains("property=\"promptText\""));
        // Штатные actions завершения сохраняются.
        assertTrue(xml.contains("action=\"windowCommitAndClose\""));
        assertTrue(xml.contains("action=\"windowClose\""));
    }

    @Test
    public void screenMessagesContainNewCaptions() throws IOException {
        String messages = readProjectFile(SCREEN + "messages.properties");
        String messagesRu = readProjectFile(SCREEN + "messages_ru.properties");

        String[] keys = {"templateCodeCaption", "templateTemperatureCaption"};
        for (String key : keys) {
            assertTrue(key + " отсутствует в messages.properties", messages.contains(key + "="));
            assertTrue(key + " отсутствует в messages_ru.properties", messagesRu.contains(key + "="));
        }
    }

    @Test
    public void everyThemeAppliesLocalScss() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/" + THEME_PARTIAL);
        assertTrue("Канон SCSS пуст или не содержит mixin",
                canon.contains("@mixin vacancy-prompt-template-editor-theme"));
        assertTrue("Нет фирменного тёмного фона #172638", canon.contains("#172638"));
        assertTrue("Нет канонического active #ffb11b", canon.contains("#ffb11b"));
        assertTrue("Нет канонического hover rgba(255,255,255,0.08)",
                canon.contains("rgba(255, 255, 255, 0.08)"));
        assertTrue("Нет канонического активного фона rgba(255,177,27,0.12)",
                canon.contains("rgba(255, 177, 27, 0.12)"));
        // Полоса-заголовок навигации «Разделы» (контракт §4.1).
        assertTrue("Нет правила полосы-заголовка .vacancy-prompt-template-navigation-title",
                canon.contains(".vacancy-prompt-template-navigation-title"));
        assertTrue("Нет inset-линий полосы-заголовка (box-shadow)",
                canon.contains("rgba(255, 255, 255, 1) 0 1px 0 0 inset"));
        assertTrue("Нет разделителя полосы-заголовка (border-bottom)",
                canon.contains("border-bottom: 1px solid rgba(255, 255, 255, 0.14)"));
        assertTrue("Нет min-height 36px полосы-заголовка",
                canon.contains("min-height: 36px !important;"));

        // Правая рабочая область по эталону IteractionListEdit.
        assertTrue("Нет карточек .edit-card с радиусом 8px", canon.contains("border-radius: 8px"));
        assertTrue("Нет заголовка секции .v-groupbox-caption", canon.contains(".v-groupbox-caption"));
        assertTrue("Нет полей 38px (.edit-card .v-textfield)",
                canon.contains(".edit-card .v-textfield"));
        assertTrue("Нет фокуса полей с $v-selection-color",
                canon.contains("rgba($v-selection-color, 0.20)"));
        assertTrue("Нет подписей .v-caption .v-captiontext",
                canon.contains(".v-caption .v-captiontext"));

        for (String theme : THEMES) {
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            assertTrue(theme + ": styles.scss не импортирует vacancy-prompt-template-editor",
                    styles.contains("vacancy-prompt-template-editor"));
            assertTrue(theme + ": styles.scss не вызывает @include vacancy-prompt-template-editor-theme",
                    styles.contains("@include vacancy-prompt-template-editor-theme;"));

            String local = readProjectFile("modules/web/themes/" + theme + "/" + THEME_PARTIAL);
            assertTrue("vacancy-prompt-template-editor.scss не идентичен в теме " + theme,
                    canon.equals(local));
        }
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
