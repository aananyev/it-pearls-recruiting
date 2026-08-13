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
 * Защищает presentation-контракт справочных Edit-форм HRM HuntTech (FileTypeEdit,
 * SocialNetworkTypeEdit, GradeEdit, CurrencyEdit, OutstaffingRatesEdit,
 * EmployeeWorkStatusEdit, SignIconsEdit): sidebar 270px, общие edit-* и label-*
 * stylename, полоса-заголовок навигации dictionary-navigation-title, карточки
 * edit-card с showAsPanel, штатная заглушка-логотип OvaFallbackImage 176×176
 * с fallback на icons/hunttech-logo.png и сохранённые data bindings/actions.
 * Бизнес-логика и loaders не проверяются.
 */
public class DictionaryEditFormsContractTest {

    private static final String SCREENS =
            "modules/web/src/com/company/hunttech/web/screens/";
    // Пары «папка экрана | имя XML-файла»: каталоги не содержат дефисов,
    // а имена дескрипторов используют kebab-case (filetype -> file-type).
    private static final String[] FORMS = {
            "filetype|file-type",
            "socialnetworktype|social-network-type",
            "grade|grade",
            "currency|currency",
            "outstaffingrates|outstaffing-rates",
            "employeeworkstatus|employee-work-status",
            "signicons|sign-icons"
    };
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
            String xml = readEditXml(form);

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
    public void everyFormUsesContractCardToolbarAndFullscreenDialog() throws IOException {
        for (String form : FORMS) {
            String xml = readEditXml(form);

            assertTrue("legacy-класс edit-section-card остался в " + form,
                    !xml.contains("edit-section-card"));
            assertTrue("legacy-класс edit-toolbar-subtitle остался в " + form,
                    !xml.contains("edit-toolbar-subtitle"));
            assertTrue(form, xml.contains("stylename=\"edit-card\""));
            assertTrue(form, xml.contains("stylename=\"edit-toolbar-description\""));
            // Карточки groupBox рендерятся как Vaadin Panel (v-panel-caption), иначе
            // CUBA-рендер c-groupbox-caption не матчит SCSS-правила контракта.
            assertTrue(form + ": edit-card без showAsPanel (заголовок карточки не стилизуется)",
                    xml.contains("showAsPanel=\"true\""));
            // Полоса-заголовок навигации «Разделы» (контракт §4.1): класс серии
            // dictionary-navigation-title поверх label-nav-title — две inset-линии.
            assertTrue(form + ": нет полосы-заголовка dictionary-navigation-title",
                    xml.contains("label-nav-title dictionary-navigation-title"));
            // Полноэкранный модальный редактор справочника.
            assertTrue(form + ": нет полноэкранного dialogMode",
                    xml.contains("<dialogMode height=\"100%\" width=\"100%\" modal=\"true\"/>"));
        }
    }

    @Test
    public void everyFormHasFallbackLogoPlaceholder176() throws IOException {
        for (String form : FORMS) {
            String xml = readEditXml(form);

            assertTrue(form + ": нет штатной заглушки-логотипа ovaFallbackImage",
                    xml.contains("<ovaFallbackImage"));
            assertTrue(form + ": логотип не 176×176",
                    xml.contains("width=\"176px\"") && xml.contains("height=\"176px\"")
                            && xml.contains("ovalWidth=\"176px\"") && xml.contains("ovalHeight=\"176px\""));
            assertTrue(form + ": нет fallback на логотип HRM HuntTech",
                    xml.contains("fallbackThemePath=\"icons/hunttech-logo.png\""));
            assertTrue(form + ": нет stylename dictionary-logo-image",
                    xml.contains("stylename=\"dictionary-logo-image\""));
        }
    }

    @Test
    public void onlySocialNetworkTypeUploadsLogo() throws IOException {
        for (String form : FORMS) {
            String xml = readEditXml(form);
            boolean hasUpload = xml.contains("<upload") || xml.contains("FileUpload");
            if (form.startsWith("socialnetworktype")) {
                assertTrue("SocialNetworkTypeEdit обязан иметь загрузку логотипа", hasUpload);
                // Аватар соцсети привязан к данным (logo) и перезаписывается после загрузки.
                assertTrue(xml.contains("id=\"snLogo\""));
                assertTrue(xml.contains("property=\"logo\""));
                assertTrue(xml.contains("id=\"snLogoFileUpload\""));
            } else {
                assertTrue(form + ": загрузка изображения не предусмотрена, upload не должен быть",
                        !hasUpload);
            }
        }
    }

    @Test
    public void everyInputFieldUsesEditFormControl() throws IOException {
        String fileType = readProjectFile(SCREENS + "filetype/file-type-edit.xml");
        assertTrue(fileType.contains(
                "id=\"nameFileTypeField\" property=\"nameFileType\" caption=\"msg://msgNameFileType\""
                        + " width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(fileType.contains(
                "id=\"decriptionFileTypeField\" property=\"decriptionFileType\""
                        + " caption=\"msg://msgDecriptionFileType\""
                        + " width=\"100%\" stylename=\"edit-form-control\""));

        String social = readProjectFile(SCREENS + "socialnetworktype/social-network-type-edit.xml");
        assertTrue(social.contains(
                "id=\"socialNetworkField\" property=\"socialNetwork\" required=\"true\""
                        + " caption=\"msg://msgSocialNetwork\" width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(social.contains(
                "id=\"socialNetworkURLField\" property=\"socialNetworkURL\" required=\"true\""
                        + " caption=\"msg://msgSocialNetworkURL\" width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(social.contains(
                "id=\"commentField\" property=\"comment\" caption=\"msg://msgComment\""
                        + " width=\"100%\" stylename=\"edit-form-control\""));

        String grade = readProjectFile(SCREENS + "grade/grade-edit.xml");
        assertTrue(grade.contains(
                "id=\"gradeNameField\" property=\"gradeName\" caption=\"msg://msgGradeName\""
                        + " width=\"100%\" stylename=\"edit-form-control\""));

        String currency = readProjectFile(SCREENS + "currency/currency-edit.xml");
        assertTrue(currency.contains(
                "id=\"currencyLongNameField\" property=\"currencyLongName\""
                        + " caption=\"msg://msgCurrencyLongName\" width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(currency.contains(
                "id=\"currencyShortNameField\" property=\"currencyShortName\""
                        + " caption=\"msg://msgCurrencyShortName\" width=\"100%\" stylename=\"edit-form-control\""));

        String rates = readProjectFile(SCREENS + "outstaffingrates/outstaffing-rates-edit.xml");
        assertTrue(rates.contains(
                "id=\"rateField\" property=\"rate\" caption=\"msg://msgRate\""
                        + " width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(rates.contains(
                "id=\"minSalaryField\" property=\"minSalary\" caption=\"msg://msgMinSalary\""
                        + " width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(rates.contains(
                "id=\"maxSalaryField\" property=\"maxSalary\" caption=\"msg://msgMaxSalary\""
                        + " width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(rates.contains(
                "id=\"maxIESalaryField\" property=\"maxIESalary\" caption=\"msg://msgMaxIESalary\""
                        + " width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(rates.contains(
                "id=\"currencyField\" optionsContainer=\"currenciesDc\" property=\"currency\""
                        + " caption=\"msg://msgCurrency\" width=\"100%\" stylename=\"edit-form-control\""));

        String status = readProjectFile(SCREENS + "employeeworkstatus/employee-work-status-edit.xml");
        assertTrue(status.contains(
                "id=\"workStatusNameField\" property=\"workStatusName\""
                        + " caption=\"msg://msgWorkStatusName\" width=\"100%\" stylename=\"edit-form-control\""));

        String icons = readProjectFile(SCREENS + "signicons/sign-icons-edit.xml");
        assertTrue(icons.contains(
                "id=\"titleEndField\" property=\"titleEnd\" caption=\"msg://msgTitleEnd\""
                        + " editable=\"false\" width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(icons.contains(
                "id=\"titleRuField\" property=\"titleRu\" caption=\"msg://msgTitleRu\""
                        + " width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(icons.contains(
                "id=\"iconNameField\" dataContainer=\"signIconsDc\""
                        + " align=\"MIDDLE_LEFT\" property=\"iconName\""
                        + " caption=\"msg://msgIconName\" width=\"100%\" stylename=\"edit-form-control\""));
        assertTrue(icons.contains(
                "id=\"titleDescription\" property=\"titleDescription\""
                        + " caption=\"msg://msgTitleDescription\" width=\"100%\" stylename=\"edit-form-control"));
    }

    @Test
    public void dataBindingsViewsLoadersAndFocusContractPreserved() throws IOException {
        String fileType = readProjectFile(SCREENS + "filetype/file-type-edit.xml");
        assertTrue(fileType.contains("id=\"fileTypeDc\""));
        assertTrue(fileType.contains("view=\"fileType-view\""));
        assertTrue(fileType.contains("focusComponent=\"nameFileTypeField\""));
        assertTrue(fileType.contains("invoke=\"focusMainSection\""));

        String social = readProjectFile(SCREENS + "socialnetworktype/social-network-type-edit.xml");
        assertTrue(social.contains("id=\"socialNetworkTypeDc\""));
        assertTrue(social.contains("view=\"socialNetworkType-view\""));
        assertTrue(social.contains("focusComponent=\"socialNetworkField\""));
        assertTrue(social.contains("invoke=\"focusMainSection\""));

        String grade = readProjectFile(SCREENS + "grade/grade-edit.xml");
        assertTrue(grade.contains("id=\"gradeDc\""));
        assertTrue(grade.contains("<view extends=\"grade-edit-view\"/>"));
        assertTrue(grade.contains("focusComponent=\"gradeNameField\""));
        assertTrue(grade.contains("invoke=\"focusMainSection\""));

        String currency = readProjectFile(SCREENS + "currency/currency-edit.xml");
        assertTrue(currency.contains("id=\"currencyDc\""));
        assertTrue(currency.contains("<view extends=\"currency-view\"/>"));
        assertTrue(currency.contains("focusComponent=\"currencyLongNameField\""));
        assertTrue(currency.contains("invoke=\"focusMainSection\""));

        String rates = readProjectFile(SCREENS + "outstaffingrates/outstaffing-rates-edit.xml");
        assertTrue(rates.contains("id=\"outstaffingRatesDc\""));
        assertTrue(rates.contains("<view extends=\"outstaffingRates-view\"/>"));
        assertTrue(rates.contains("id=\"currenciesDc\""));
        assertTrue(rates.contains("select e from hunttech_Currency e"));
        assertTrue(rates.contains("focusComponent=\"rateField\""));
        assertTrue(rates.contains("invoke=\"focusRatesSection\""));
        assertTrue(rates.contains("invoke=\"focusCommentSection\""));

        String status = readProjectFile(SCREENS + "employeeworkstatus/employee-work-status-edit.xml");
        assertTrue(status.contains("id=\"employeeWorkStatusDc\""));
        assertTrue(status.contains("<view extends=\"employeeWorkStatus-view\"/>"));
        assertTrue(status.contains("focusComponent=\"workStatusNameField\""));
        assertTrue(status.contains("invoke=\"focusMainSection\""));

        String icons = readProjectFile(SCREENS + "signicons/sign-icons-edit.xml");
        assertTrue(icons.contains("id=\"signIconsDc\""));
        assertTrue(icons.contains("<view extends=\"signIcons-view\"/>"));
        assertTrue(icons.contains("focusComponent=\"iconNameField\""));
        assertTrue(icons.contains("invoke=\"focusIconSection\""));
        assertTrue(icons.contains("invoke=\"focusDescriptionSection\""));
    }

    @Test
    public void screenMessagePacksContainFieldCaptions() throws IOException {
        String[][] packs = {
                {"filetype", "msgNameFileType", "msgDecriptionFileType"},
                {"socialnetworktype", "msgSocialNetwork", "msgSocialNetworkURL", "msgComment"},
                {"grade", "msgGradeName"},
                {"currency", "msgCurrencyLongName", "msgCurrencyShortName"},
                {"outstaffingrates", "msgRate", "msgMinSalary", "msgMaxSalary", "msgMaxIESalary", "msgCurrency", "msgComment"},
                {"employeeworkstatus", "msgInStaff", "msgWorkStatusName"},
                {"signicons", "msgTitleEnd", "msgTitleRu", "msgIconName", "msgIconColor", "msgTitleDescription"}
        };
        for (String[] pack : packs) {
            String base = SCREENS + pack[0] + "/messages";
            String messages = readProjectFile(base + ".properties");
            String messagesRu = readProjectFile(base + "_ru.properties");
            for (int i = 1; i < pack.length; i++) {
                String key = pack[i];
                assertTrue(pack[0] + ": ключ " + key + " отсутствует в messages.properties",
                        messages.contains(key + "="));
                assertTrue(pack[0] + ": ключ " + key + " отсутствует в messages_ru.properties",
                        messagesRu.contains(key + "="));
            }
        }
    }

    @Test
    public void everyThemeAppliesDictionaryLocalScss() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/dictionary-edit-forms.scss");
        assertTrue("Канон справочного SCSS пуст или не содержит mixin",
                canon.contains("@mixin dictionary-edit-forms-theme"));
        assertTrue("Нет фирменного тёмного фона #172638", canon.contains("#172638"));
        assertTrue("Нет канонического active #ffb11b", canon.contains("#ffb11b"));
        assertTrue("Нет канонического hover rgba(255,255,255,0.08)",
                canon.contains("rgba(255, 255, 255, 0.08)"));
        assertTrue("Нет канонического активного фона rgba(255,177,27,0.12)",
                canon.contains("rgba(255, 177, 27, 0.12)"));
        // Полоса-заголовок навигации «Разделы» (контракт §4.1): две inset-линии.
        assertTrue("Нет правила полосы-заголовка .dictionary-navigation-title",
                canon.contains(".dictionary-navigation-title"));
        assertTrue("Нет inset-линий полосы-заголовка (box-shadow)",
                canon.contains("rgba(255, 255, 255, 1) 0 1px 0 0 inset"));
        assertTrue("Нет разделителя полосы-заголовка (border-bottom)",
                canon.contains("border-bottom: 1px solid rgba(255, 255, 255, 0.14)"));
        assertTrue("Нет цвета полосы-заголовка #ffb11b 15px/700",
                canon.contains("color: #ffb11b !important;"));
        assertTrue("Нет min-height 36px полосы-заголовка",
                canon.contains("min-height: 36px !important;"));
        // Штатная заглушка-логотип 176×176 (размер candidatePic JobCandidateEdit).
        assertTrue("Нет правила аватара .dictionary-logo-image",
                canon.contains(".dictionary-logo-image"));
        assertTrue("Нет размера 176px у аватара",
                canon.contains("176px"));
        assertTrue("Нет object-fit contain у аватара",
                canon.contains("object-fit: contain"));

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
            assertTrue(theme + ": styles.scss не импортирует dictionary-edit-forms",
                    styles.contains("dictionary-edit-forms"));
            assertTrue(theme + ": styles.scss не вызывает @include dictionary-edit-forms-theme",
                    styles.contains("@include dictionary-edit-forms-theme;"));

            String local = readProjectFile(
                    "modules/web/themes/" + theme + "/com.company.hunttech/dictionary-edit-forms.scss");
            assertTrue("dictionary-edit-forms.scss не идентичен в теме " + theme, canon.equals(local));
        }
    }

    private String readEditXml(String form) throws IOException {
        String[] parts = form.split("\\|");
        return readProjectFile(SCREENS + parts[0] + "/" + parts[1] + "-edit.xml");
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
