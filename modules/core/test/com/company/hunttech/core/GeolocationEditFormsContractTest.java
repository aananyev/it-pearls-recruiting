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
 * Защищает presentation-контракт гео-справочников HRM HuntTech (CountryEdit,
 * RegionEdit, CityEdit): sidebar 270px, общие edit-* и label-* stylename,
 * единый стиль полей edit-form-control и сохранённые data bindings/actions.
 * Бизнес-логика и loaders не проверяются.
 */
public class GeolocationEditFormsContractTest {

    private static final String SCREENS =
            "modules/web/src/com/company/hunttech/web/screens/";
    private static final String[] FORMS = {"country", "region", "city"};
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
    public void everyFormUsesSharedSidebarAndWorkspaceOrder() throws IOException {
        for (String form : FORMS) {
            String xml = readProjectFile(SCREENS + form + "/" + form + "-edit.xml");

            assertTrue(form, xml.contains("stylename=\"edit-sidebar\""));
            assertTrue(form, xml.contains("width=\"270px\""));
            assertTrue(form, xml.contains("stylename=\"edit-screen-layout\""));
            assertTrue(form, xml.contains("stylename=\"edit-workspace\""));
            assertTrue(form, xml.contains("stylename=\"label-navigation\""));
            assertTrue(form, xml.contains("label-nav-title"));
            assertTrue(form, xml.contains("label-nav-item label-nav-item-active"));
            assertTrue(form, xml.contains("stylename=\"edit-footer-actions\""));
        }
    }

    @Test
    public void everyFormUsesContractCardAndToolbarClasses() throws IOException {
        for (String form : FORMS) {
            String xml = readProjectFile(SCREENS + form + "/" + form + "-edit.xml");

            assertTrue("legacy-класс edit-section-card остался в " + form,
                    !xml.contains("edit-section-card"));
            assertTrue("legacy-класс edit-toolbar-subtitle остался в " + form,
                    !xml.contains("edit-toolbar-subtitle"));
            assertTrue(form, xml.contains("stylename=\"edit-card\""));
            assertTrue(form, xml.contains("stylename=\"edit-toolbar-description\""));
            // Карточки groupBox рендерятся как Vaadin Panel (v-panel-caption), иначе
            // CUBA-рендер c-groupbox-caption не матчит SCSS-правила контракта
            // (эталон: showAsPanel у всех edit-card).
            assertTrue(form + ": edit-card без showAsPanel (заголовок карточки не стилизуется)",
                    xml.contains("showAsPanel=\"true\""));
            // Полоса-заголовок навигации «Разделы» (контракт §4.1): класс секции
            // поверх label-nav-title — две горизонтальные inset-линии.
            assertTrue(form + ": нет полосы-заголовка geolocation-navigation-title",
                    xml.contains("label-nav-title geolocation-navigation-title"));
        }
    }

    @Test
    public void everyInputFieldUsesEditFormControl() throws IOException {
        String country = readProjectFile(SCREENS + "country/country-edit.xml");
        assertTrue(country.contains(
                "id=\"countryRuNameField\" property=\"countryRuName\" caption=\"msg://msgCountry\""
                        + " width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(country.contains(
                "id=\"countryShortNameField\" property=\"countryShortName\""
                        + " caption=\"msg://msgCountryShortName\""
                        + " width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(country.contains(
                "id=\"phoneCodeField\" property=\"phoneCode\""
                        + " caption=\"msg://msgPhoneCode\""
                        + " width=\"100%\" stylename=\"edit-form-control\""));

        String region = readProjectFile(SCREENS + "region/region-edit.xml");
        assertTrue(region.contains(
                "id=\"regionRuNameField\" property=\"regionRuName\""
                        + " caption=\"mainMsg://msgRegionRuName\" width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(region.contains(
                "id=\"regionCountryField\" optionsContainer=\"regionCountriesDc\" property=\"regionCountry\""
                        + " caption=\"mainMsg://msgCountry\" width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(region.contains(
                "id=\"regionCodeField\" property=\"regionCode\""
                        + " caption=\"mainMsg://msgRegionCode\" width=\"100%\" stylename=\"edit-form-control\""));

        String city = readProjectFile(SCREENS + "city/city-edit.xml");
        assertTrue(city.contains(
                "id=\"cityRuNameField\" property=\"cityRuName\""
                        + " caption=\"mainMsg://msgCityRuName\" width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(city.contains(
                "id=\"cityPhoneCodeField\" property=\"cityPhoneCode\""
                        + " caption=\"mainMsg://msgCityPhoneCode\" width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(city.contains(
                "id=\"cityRegionField\" optionsContainer=\"cityRegionsDc\" property=\"cityRegion\""
                        + " caption=\"mainMsg://msgCityRegion\" width=\"100%\" stylename=\"edit-form-control\""));
    }

    @Test
    public void tableColumnsKeepCaptionsAndCompositionActions() throws IOException {
        String country = readProjectFile(SCREENS + "country/country-edit.xml");
        assertTrue(country.contains("column id=\"regionRuName\" caption=\"mainMsg://msgRegionRuName\""));
        assertTrue(country.contains("column id=\"regionCode\" caption=\"mainMsg://msgRegionCode\""));
        assertTrue(country.contains("id=\"countryCountryOfRegionTable\""));
        assertTrue(country.contains("action id=\"add\" type=\"add\""));
        assertTrue(country.contains("action id=\"edit\" type=\"edit\""));
        assertTrue(country.contains("action id=\"remove\" type=\"remove\""));

        String region = readProjectFile(SCREENS + "region/region-edit.xml");
        assertTrue(region.contains("column id=\"cityRuName\" caption=\"mainMsg://msgCityRuName\""));
        assertTrue(region.contains("column id=\"cityPhoneCode\" caption=\"mainMsg://msgCityPhoneCode\""));
        assertTrue(region.contains("id=\"regionRegionOfCityTable\""));
        assertTrue(region.contains("action id=\"add\" type=\"add\""));
        assertTrue(region.contains("action id=\"edit\" type=\"edit\""));
        assertTrue(region.contains("action id=\"remove\" type=\"remove\""));
    }

    @Test
    public void dataBindingsViewsLoadersAndFocusContractPreserved() throws IOException {
        String country = readProjectFile(SCREENS + "country/country-edit.xml");
        assertTrue(country.contains("id=\"countryDc\""));
        assertTrue(country.contains("view=\"country-edit-view\""));
        assertTrue(country.contains("id=\"countryCountryOfRegionsDc\""));
        assertTrue(country.contains("property=\"countryOfRegion\""));
        assertTrue(country.contains("focusComponent=\"countryRuNameField\""));
        assertTrue(country.contains("invoke=\"focusMainSection\""));
        assertTrue(country.contains("invoke=\"focusRegionsSection\""));

        String region = readProjectFile(SCREENS + "region/region-edit.xml");
        assertTrue(region.contains("id=\"regionDc\""));
        assertTrue(region.contains("view=\"region-edit-view\""));
        assertTrue(region.contains("id=\"regionRegionOfCitiesDc\" property=\"regionOfCity\""));
        assertTrue(region.contains("id=\"regionCountriesLc\" cacheable=\"true\""));
        assertTrue(region.contains("focusComponent=\"regionRuNameField\""));
        assertTrue(region.contains("invoke=\"focusCitiesSection\""));

        String city = readProjectFile(SCREENS + "city/city-edit.xml");
        assertTrue(city.contains("id=\"cityDc\""));
        assertTrue(city.contains("view=\"city-edit-view\""));
        assertTrue(city.contains("id=\"cityRegionsLc\" cacheable=\"true\""));
        assertTrue(city.contains("property=\"cityRegion.regionRuName\""));
        assertTrue(city.contains("focusComponent=\"cityRuNameField\""));
        assertTrue(city.contains("invoke=\"focusMainSection\""));
    }

    @Test
    public void mainMessagesContainGeoCaptions() throws IOException {
        String messages = readProjectFile(
                "modules/web/src/com/company/hunttech/web/messages.properties");
        String messagesRu = readProjectFile(
                "modules/web/src/com/company/hunttech/web/messages_ru.properties");

        String[] keys = {"msgRegionRuName", "msgCityRuName", "msgCityPhoneCode", "msgCityRegion"};
        for (String key : keys) {
            assertTrue(key + " отсутствует в messages.properties", messages.contains(key + "="));
            assertTrue(key + " отсутствует в messages_ru.properties", messagesRu.contains(key + "="));
        }
    }

    @Test
    public void localMessagesContainSidebarCaptions() throws IOException {
        // Подписи sidebar («Регион»/«Город») переводятся msg-ключами локальных пакетов
        // (в главных messages msgRegion=«Область» — для подписи типа записи не подходит).
        String[][] packs = {
                {"region", "msgRegion"},
                {"city", "msgCity"},
                {"city", "msgRegion"} // подпись сводки «Регион» в sidebar города
        };
        for (String[] p : packs) {
            String base = "modules/web/src/com/company/hunttech/web/screens/" + p[0] + "/messages";
            assertTrue(p[0] + " " + p[1] + " отсутствует в messages.properties",
                    readProjectFile(base + ".properties").contains(p[1] + "="));
            assertTrue(p[0] + " " + p[1] + " отсутствует в messages_ru.properties",
                    readProjectFile(base + "_ru.properties").contains(p[1] + "="));
        }
    }

    @Test
    public void everyThemeAppliesSharedEditStyles() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/edit-screen-shared-styles.scss");
        for (String theme : THEMES) {
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            assertTrue(theme + ": styles.scss не импортирует edit-screen-shared-styles",
                    styles.contains("edit-screen-shared-styles"));
            assertTrue(theme + ": styles.scss не вызывает @include edit-screen-shared-styles",
                    styles.contains("@include edit-screen-shared-styles"));

            String shared = readProjectFile(
                    "modules/web/themes/" + theme + "/com.company.hunttech/edit-screen-shared-styles.scss");
            assertTrue("edit-screen-shared-styles.scss не идентичен в теме " + theme, canon.equals(shared));

            // Классы, на которые опираются гео-формы, обязаны присутствовать в общем mixin.
            assertTrue(theme + ": нет .edit-sidebar (270px)",
                    shared.contains(".edit-sidebar,") || shared.contains(".edit-sidebar {"));
            assertTrue(theme + ": нет .edit-card", shared.contains(".edit-card"));
            assertTrue(theme + ": нет .edit-form-control", shared.contains(".edit-form-control"));
            assertTrue(theme + ": нет .label-nav-item", shared.contains(".label-nav-item"));
            assertTrue(theme + ": нет .edit-toolbar-description",
                    shared.contains(".edit-toolbar-description"));
        }
    }

    @Test
    public void everyThemeAppliesGeolocationLocalScss() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/geolocation-edit-forms.scss");
        assertTrue("Канон гео-SCSS пуст или не содержит mixin", canon.contains("@mixin geolocation-edit-forms-theme"));
        assertTrue("Нет фирменного тёмного фона #172638", canon.contains("#172638"));
        assertTrue("Нет канонического active #ffb11b", canon.contains("#ffb11b"));
        assertTrue("Нет канонического hover rgba(255,255,255,0.08)",
                canon.contains("rgba(255, 255, 255, 0.08)"));
        assertTrue("Нет канонического активного фона rgba(255,177,27,0.12)",
                canon.contains("rgba(255, 177, 27, 0.12)"));
        // Полоса-заголовок навигации «Разделы» (контракт §4.1): две inset-линии.
        assertTrue("Нет правила полосы-заголовка .geolocation-navigation-title",
                canon.contains(".geolocation-navigation-title"));
        assertTrue("Нет inset-линий полосы-заголовка (box-shadow)",
                canon.contains("rgba(255, 255, 255, 1) 0 1px 0 0 inset"));
        assertTrue("Нет разделителя полосы-заголовка (border-bottom)",
                canon.contains("border-bottom: 1px solid rgba(255, 255, 255, 0.14)"));
        assertTrue("Нет цвета полосы-заголовка #ffb11b 15px/700",
                canon.contains("color: #ffb11b !important;"));
        assertTrue("Нет min-height 36px полосы-заголовка",
                canon.contains("min-height: 36px !important;"));
        // Nav-кнопки sidebar — ровно 27px (фикс высоты Vaadin-кнопки, эталон IteractionListEdit;
        // иначе halo-тема раздувает пункты до 46px).
        assertTrue("Нет фикса высоты nav-кнопки 27px (.v-button-label-nav-item)",
                canon.contains("height: 27px !important"));
        assertTrue("Нет flex-wrap центрирования .v-button-label-nav-item .v-button-wrap",
                canon.contains(".v-button-label-nav-item .v-button-wrap"));

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
            assertTrue(theme + ": styles.scss не импортирует geolocation-edit-forms",
                    styles.contains("geolocation-edit-forms"));
            assertTrue(theme + ": styles.scss не вызывает @include geolocation-edit-forms-theme",
                    styles.contains("@include geolocation-edit-forms-theme;"));

            String local = readProjectFile(
                    "modules/web/themes/" + theme + "/com.company.hunttech/geolocation-edit-forms.scss");
            assertTrue("geolocation-edit-forms.scss не идентичен в теме " + theme, canon.equals(local));
        }
    }

    @Test
    public void countrySidebarDropsTypeSubtitleAndCentersTitle() throws IOException {
        // По запросу «убери слово "Страна"» из sidebar: подпись типа записи
        // (edit-sidebar-subtitle) удалена, остаётся только название страны.
        String country = readProjectFile(SCREENS + "country/country-edit.xml");
        assertTrue("Слово «Страна» (edit-sidebar-subtitle) осталось в sidebar CountryEdit",
                !country.contains("edit-sidebar-subtitle"));
        assertTrue(country.contains("id=\"countrySidebarTitle\""));
        assertTrue(country.contains("stylename=\"edit-sidebar-title\""));

        // Название страны выровнено по центру по горизонтали (эталон ProjectEdit).
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/geolocation-edit-forms.scss");
        assertTrue("Нет центрирования .edit-sidebar-title (text-align: center)",
                canon.contains("text-align: center !important"));
        // Стандартные отступы контента sidebar от краёв (эталон ProjectEdit).
        assertTrue("Нет стандартного padding sidebar 14px 16px 12px",
                canon.contains("padding: 14px 16px 12px !important"));
        assertTrue("Нет правой границы sidebar",
                canon.contains("border-right: 1px solid rgba(15, 23, 42, 0.78) !important"));
    }

    @Test
    public void countryFooterActionsUseProjectStyle() throws IOException {
        // Кнопки ОК/Отмена прижаты к правому нижнему углу (expand + MIDDLE_RIGHT)
        // и стилизованы как у ProjectEdit: primary/secondary классы в XML.
        String country = readProjectFile(SCREENS + "country/country-edit.xml");
        assertTrue(country.contains("expand=\"countryActionsSpacer\""));
        assertTrue(country.contains("align=\"MIDDLE_RIGHT\""));
        assertTrue(country.contains("stylename=\"country-editor-primary-action\""));
        assertTrue(country.contains("stylename=\"country-editor-secondary-action\""));

        // SCSS-слой: панель и кнопки 40px/14px/600/radius 4px, primary/secondary
        // цвета — 1:1 с project-editor.scss.
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/geolocation-edit-forms.scss");
        assertTrue("Нет стилей .edit-footer-actions", canon.contains(".edit-footer-actions"));
        assertTrue("Нет кнопок 40px footer", canon.contains("min-height: 40px !important"));
        assertTrue("Нет .country-editor-primary-action", canon.contains(".country-editor-primary-action"));
        assertTrue("Нет .country-editor-secondary-action", canon.contains(".country-editor-secondary-action"));
        assertTrue("Нет акцентной заливки primary ($v-selection-color)",
                canon.contains("background: $v-selection-color !important"));
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
