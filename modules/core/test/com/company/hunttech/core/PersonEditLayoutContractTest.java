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
 * Защищает presentation-контракт справочника «Люди» (PersonEdit) HRM HuntTech:
 * двухпанельная композиция по общему контракту Edit-экранов (sidebar 270px,
 * label-навигация, карточки edit-card, footer primary/secondary), круглый аватар
 * ovaFallbackImage с загрузкой фото и сохранённые data bindings/actions.
 * Бизнес-логика и loaders не проверяются.
 */
public class PersonEditLayoutContractTest {

    private static final String SCREENS =
            "modules/web/src/com/company/hunttech/web/screens/";
    private static final String PERSON_XML = SCREENS + "person/person-edit.xml";
    private static final String PERSON_JAVA = SCREENS + "person/PersonEdit.java";
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
    public void usesSharedSidebarAndWorkspaceComposition() throws IOException {
        String xml = readProjectFile(PERSON_XML);

        assertTrue(xml.contains("stylename=\"edit-sidebar\""));
        assertTrue("Sidebar должен быть 270px (контракт §4.2 для справочников)",
                xml.contains("width=\"270px\""));
        assertTrue(xml.contains("stylename=\"edit-screen-layout\""));
        assertTrue(xml.contains("stylename=\"edit-workspace\""));
        assertTrue(xml.contains("stylename=\"label-navigation\""));
        assertTrue(xml.contains("label-nav-title person-navigation-title"));
        assertTrue(xml.contains("label-nav-item label-nav-item-active"));
        assertTrue(xml.contains("stylename=\"edit-footer-actions\""));
        assertTrue(xml.contains("stylename=\"person-editor\""));
    }

    @Test
    public void usesSidebarVisualWithOvalImageAndUpload() throws IOException {
        String xml = readProjectFile(PERSON_XML);

        // Круглый аватар 176×176 с fallback-картинкой (эталон SkillTreeEdit).
        assertTrue(xml.contains("<ovaFallbackImage id=\"personPic\""));
        assertTrue(xml.contains("property=\"fileImageFace\""));
        assertTrue(xml.contains("width=\"176px\""));
        assertTrue(xml.contains("height=\"176px\""));
        assertTrue(xml.contains("ovalWidth=\"176px\""));
        assertTrue(xml.contains("ovalHeight=\"176px\""));
        assertTrue(xml.contains("fallbackThemePath=\"icons/no-programmer.jpeg\""));
        assertTrue(xml.contains("stylename=\"person-logo-image\""));

        // Загрузка фото: IMMEDIATE, dropZone на блок визуала, биндинг на fileImageFace.
        assertTrue(xml.contains("id=\"fileImageFaceUpload\""));
        assertTrue(xml.contains("fileStoragePutMode=\"IMMEDIATE\""));
        assertTrue(xml.contains("dropZone=\"personVisual\""));
        assertTrue(xml.contains("showClearButton=\"true\""));

        // Старый дубль image+defaultPeoplePic заменён на ovaFallbackImage.
        assertTrue("legacy-элемент defaultPeoplePic остался в XML",
                !xml.contains("defaultPeoplePic"));
        assertTrue("legacy-класс dropzone-container остался в XML",
                !xml.contains("dropzone-container"));
    }

    @Test
    public void identityDropsTypeSubtitleAndCentersTitle() throws IOException {
        String xml = readProjectFile(PERSON_XML);

        // Подпись типа записи в sidebar отсутствует (паттерн серии гео-форм
        // 2026-08-14: «убери слово …» из identity).
        assertTrue("edit-sidebar-subtitle остался в sidebar PersonEdit",
                !xml.contains("edit-sidebar-subtitle"));
        assertTrue(xml.contains("id=\"personSidebarTitle\""));
        assertTrue(xml.contains("property=\"firstName\""));
        assertTrue(xml.contains("stylename=\"edit-sidebar-title\""));

        // Центрирование title по горизонтали — в локальном SCSS-слое.
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/person-editor.scss");
        assertTrue("Нет центрирования .edit-sidebar-title (text-align: center)",
                canon.contains("text-align: center !important"));
        // Стандартные отступы контента sidebar (эталон ProjectEdit/гео-форм).
        assertTrue("Нет стандартного padding sidebar 14px 16px 12px",
                canon.contains("padding: 14px 16px 12px !important"));
        assertTrue("Нет правой границы sidebar",
                canon.contains("border-right: 1px solid rgba(15, 23, 42, 0.78) !important"));
    }

    @Test
    public void footerActionsUseProjectStyle() throws IOException {
        String xml = readProjectFile(PERSON_XML);

        // Кнопки ОК/Отмена прижаты к правому нижнему углу (expand + MIDDLE_RIGHT)
        // и стилизованы как у ProjectEdit: primary/secondary классы в XML.
        assertTrue(xml.contains("expand=\"personActionsSpacer\""));
        assertTrue(xml.contains("align=\"MIDDLE_RIGHT\""));
        assertTrue(xml.contains("stylename=\"person-editor-primary-action\""));
        assertTrue(xml.contains("stylename=\"person-editor-secondary-action\""));

        // SCSS-слой: панель и кнопки 40px/14px/600/radius 4px, primary/secondary
        // цвета — 1:1 с project-editor.scss (эталон).
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/person-editor.scss");
        assertTrue("Нет стилей .edit-footer-actions", canon.contains(".edit-footer-actions"));
        assertTrue("Нет кнопок 40px footer", canon.contains("min-height: 40px !important"));
        assertTrue("Нет .person-editor-primary-action",
                canon.contains(".person-editor-primary-action"));
        assertTrue("Нет .person-editor-secondary-action",
                canon.contains(".person-editor-secondary-action"));
        assertTrue("Нет акцентной заливки primary ($v-selection-color)",
                canon.contains("background: $v-selection-color !important"));
    }

    @Test
    public void dataBindingsViewsLoadersAndFocusContractPreserved() throws IOException {
        String xml = readProjectFile(PERSON_XML);

        // Контейнеры и views не изменились.
        assertTrue(xml.contains("id=\"personDc\""));
        assertTrue(xml.contains("view=\"person-edit-view\""));
        assertTrue(xml.contains("id=\"positionCityLc\" cacheable=\"true\""));
        assertTrue(xml.contains("id=\"positionCountriesLc\" cacheable=\"true\""));
        assertTrue(xml.contains("id=\"personPositionsLc\" cacheable=\"true\""));

        // Все поля формы сохранены с property-биндингами (проверка независимая
        // от порядка атрибутов и переносов строк в XML).
        String[][] fields = {
                {"firstNameField", "firstName"},
                {"middleNameField", "middleName"},
                {"secondNameField", "secondName"},
                {"birdhDateField", "birdhDate"},
                {"emailField", "email"},
                {"phoneField", "phone"},
                {"mobilePhoneField", "mobPhone"},
                {"skypeNameField", "skypeName"},
                {"telegramNameField", "telegramName"},
                {"wiberNameField", "wiberName"},
                {"watsupNameField", "watsupName"},
                {"positionCityField", "cityOfResidence"},
                {"positionCountryField", "positionCountry"},
                {"personPositionField", "personPosition"}
        };
        for (String[] f : fields) {
            assertTrue("Поле не найдено: " + f[0], xml.contains("id=\"" + f[0] + "\""));
            assertTrue("Биндинг property не найден: " + f[1],
                    xml.contains("property=\"" + f[1] + "\""));
        }

        // Пейкеры сохранили lookup-actions и optionsContainer.
        assertTrue(xml.contains("optionsContainer=\"positionCityDc\""));
        assertTrue(xml.contains("optionsContainer=\"positionCountriesDc\""));
        assertTrue(xml.contains("optionsContainer=\"personPositionsDc\""));
        assertTrue(xml.contains("type=\"picker_lookup\""));

        // Фокус и презентационная навигация.
        assertTrue(xml.contains("focusComponent=\"firstNameField\""));
        assertTrue(xml.contains("invoke=\"focusMainSection\""));
        assertTrue(xml.contains("invoke=\"focusContactsSection\""));
        assertTrue(xml.contains("invoke=\"focusLocationSection\""));
    }

    @Test
    public void javaControllerKeepsUploadHandlersAndAddsNavigation() throws IOException {
        String java = readProjectFile(PERSON_JAVA);

        // Обработчик успешной загрузки фото сохранён (эталон SkillTreeEdit).
        assertTrue(java.contains("onFileImageFaceUploadFileUploadSucceed"));
        assertTrue(java.contains("FileDescriptorResource"));
        assertTrue(java.contains("personPic.createResource"));
        // Fallback-аватар при отсутствии файла (эталон SkillTreeEdit onAfterShow).
        assertTrue(java.contains("onAfterShow(AfterShowEvent"));
        assertTrue(java.contains("personPic.applyFallback()"));
        assertTrue(java.contains("getEditedEntity().getFileImageFace() == null"));

        // Презентационная навигация без изменения данных.
        assertTrue(java.contains("public void focusMainSection()"));
        assertTrue(java.contains("public void focusContactsSection()"));
        assertTrue(java.contains("public void focusLocationSection()"));
        assertTrue(java.contains("label-nav-item-active"));

        // Старые переключатели двух image удалены.
        assertTrue("setPeoplePicImage остался в контроллере", !java.contains("setPeoplePicImage"));
        assertTrue("defaultPeoplePic остался в контроллере", !java.contains("defaultPeoplePic"));
    }

    @Test
    public void everyThemeAppliesPersonLocalScss() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/person-editor.scss");
        assertTrue("Канон person-SCSS пуст или не содержит mixin",
                canon.contains("@mixin person-editor-theme"));
        assertTrue("Нет фирменного тёмного фона #172638", canon.contains("#172638"));
        assertTrue("Нет канонического active #ffb11b", canon.contains("#ffb11b"));
        assertTrue("Нет канонического hover rgba(255,255,255,0.08)",
                canon.contains("rgba(255, 255, 255, 0.08)"));
        assertTrue("Нет канонического активного фона rgba(255,177,27,0.12)",
                canon.contains("rgba(255, 177, 27, 0.12)"));
        // Полоса-заголовок навигации «Разделы» (контракт §4.1): две inset-линии.
        assertTrue("Нет правила полосы-заголовка .person-navigation-title",
                canon.contains(".person-navigation-title"));
        assertTrue("Нет inset-линий полосы-заголовка (box-shadow)",
                canon.contains("rgba(255, 255, 255, 1) 0 1px 0 0 inset"));
        // Nav-кнопки sidebar — ровно 27px (фикс высоты Vaadin-кнопки, эталон IteractionListEdit).
        assertTrue("Нет фикса высоты nav-кнопки 27px (.v-button-label-nav-item)",
                canon.contains("height: 27px !important"));
        // Круглый аватар 176px.
        assertTrue("Нет геометрии аватара 176px (.person-logo-image)",
                canon.contains(".person-logo-image"));
        // Правая рабочая область по эталону IteractionListEdit.
        assertTrue("Нет карточек .edit-card с радиусом 8px", canon.contains("border-radius: 8px"));
        assertTrue("Нет полей 38px (.edit-card .v-textfield)",
                canon.contains(".edit-card .v-textfield"));
        assertTrue("Нет фокуса полей с $v-selection-color",
                canon.contains("rgba($v-selection-color, 0.20)"));
        assertTrue("Нет подписей .v-caption .v-captiontext",
                canon.contains(".v-caption .v-captiontext"));

        for (String theme : THEMES) {
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            assertTrue(theme + ": styles.scss не импортирует person-editor",
                    styles.contains("com.company.hunttech/person-editor"));
            assertTrue(theme + ": styles.scss не вызывает @include person-editor-theme",
                    styles.contains("@include person-editor-theme;"));

            String local = readProjectFile(
                    "modules/web/themes/" + theme + "/com.company.hunttech/person-editor.scss");
            assertTrue("person-editor.scss не идентичен в теме " + theme, canon.equals(local));
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
