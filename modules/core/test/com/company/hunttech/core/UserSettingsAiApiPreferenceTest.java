package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.entity.UserSettings;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import org.junit.ClassRule;
import org.junit.Test;

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

public class UserSettingsAiApiPreferenceTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    private final Metadata metadata = AppBeans.get(Metadata.class);

    @Test
    public void userSettingsContainsPersonalAiApiPreference() {
        MetaClass metaClass = metadata.getClassNN("hunttech_UserSettings");
        assertNotNull(metaClass.getPropertyNN("preferPersonalAiApiSettings"));
        assertEquals(Boolean.class,
                metaClass.getPropertyNN("preferPersonalAiApiSettings").getJavaType());
    }

    @Test
    public void newUserSettingsKeepsAdministrativeApiRouteByDefault() {
        UserSettings settings = metadata.create(UserSettings.class);
        assertEquals(Boolean.FALSE, settings.getPreferPersonalAiApiSettings());
    }

    @Test
    public void settingsWindowBindsCheckboxToUserSettings() throws IOException {
        String screenXml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml");
        assertTrue(screenXml.contains("id=\"preferPersonalAiApiSettingsField\""));
        assertTrue(screenXml.contains("datasource=\"userSettingsDs\""));
        assertTrue(screenXml.contains("property=\"preferPersonalAiApiSettings\""));
    }

    @Test
    public void settingsWindowAiTabUsesEditDesignWithoutChangingBehaviorContracts() throws IOException {
        /*
         * Дизайн вкладки может меняться только через контейнеры и локальные стили.
         * Проверка закрепляет существующие действия и datasource, чтобы визуальный
         * рефакторинг не изменил бизнес-поведение персональных AI-подключений.
         */
        String screenXml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml");

        assertTrue(screenXml.contains("id=\"aiSettingsMainBox\""));
        assertTrue(screenXml.contains("stylename=\"ai-settings-editor\""));
        assertTrue(screenXml.contains("id=\"aiSettingsSidebar\""));
        assertTrue(screenXml.contains("width=\"270px\""));
        assertTrue(screenXml.contains("id=\"aiConfigsButtonsPanel\""));
        assertTrue(screenXml.contains("invoke=\"onAiConfigsCreateBtnClick\""));
        assertTrue(screenXml.contains("invoke=\"onAiConfigsEditBtnClick\""));
        assertTrue(screenXml.contains("invoke=\"onAiConfigsRemoveBtnClick\""));
        assertTrue(screenXml.contains("invoke=\"onAiConfigsTestBtnClick\""));
        assertTrue(screenXml.contains("id=\"aiConfigsTable\""));
        assertTrue(screenXml.contains("<rows datasource=\"userAiConfigsDs\"/>"));
    }

    @Test
    public void settingsWindowInterfaceAndEmailTabsUseEditDesignWithoutChangingCubaContracts()
            throws IOException {
        /*
         * Регрессия фиксирует CUBA-контракты двух legacy-вкладок: базовый SettingsWindow
         * находит интерфейсные компоненты по ID, а ExtSettingsWindow вручную читает
         * почтовые TextField и CheckBox. Разрешено менять только визуальные контейнеры.
         */
        String screenXml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml");

        assertTrue(screenXml.contains("stylename=\"ext-settings-window\""));
        assertTrue(screenXml.contains("stylename=\"framed ext-settings-tabs\""));
        assertTrue(screenXml.contains("id=\"interfaceSettingsMainBox\""));
        assertTrue(screenXml.contains("stylename=\"interface-settings-editor\""));
        assertTrue(screenXml.contains("id=\"interfaceSettingsSidebar\""));
        assertTrue(screenXml.contains("id=\"emailSettingsMainBox\""));
        assertTrue(screenXml.contains("stylename=\"email-settings-editor\""));
        assertTrue(screenXml.contains("id=\"emailSettingsSidebar\""));

        List<String> preservedComponentIds = Arrays.asList(
                "grid", "mainWindowLabel", "modeOptions", "visualThemeLabel", "appThemeField",
                "languageLabel", "appLangField", "timeZoneLabel", "timeZoneBox",
                "timeZoneLookup", "timeZoneAutoField", "defaultScreenLabel", "defaultScreenField",
                "changePasswordBtn", "resetScreenSettingsBtn",
                "smtpServer", "smtpPort", "smtpPasswordRequired", "smtpPassword",
                "pop3Server", "pop3Port", "pop3PasswordRequired", "pop3Password",
                "imapServer", "imapPort", "imapPasswordRequired", "imapPassword");
        for (String componentId : preservedComponentIds) {
            assertTrue("Потерян CUBA component ID: " + componentId,
                    screenXml.contains("id=\"" + componentId + "\""));
        }

        assertTrue(screenXml.contains("id=\"appThemeField\" required=\"true\""));
        assertEquals(3, countOccurrences(screenXml,
                "class=\"com.haulmont.cuba.gui.components.validators.IntegerValidator\""));
        assertTrue(screenXml.contains("id=\"msgMyInfo\""));
        assertTrue(screenXml.contains("id=\"aiAccessTab\""));
    }

    @Test
    public void allSupportedThemesContainLocalizedAiSettingsStyles() throws IOException {
        List<String> themes = supportedThemes();

        for (String theme : themes) {
            String scss = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/user-ai-profile.scss");
            assertTrue("В теме " + theme + " отсутствует локальный AI-дизайн",
                    scss.contains(".ai-settings-editor"));
            assertTrue("В теме " + theme + " отсутствует карточка подключений",
                    scss.contains(".ai-settings-connections-card"));
        }
    }

    @Test
    public void allSupportedThemesContainLocalizedInterfaceAndEmailStyles() throws IOException {
        /*
         * Визуальный контракт должен быть одинаковым во всех поддерживаемых темах.
         * Стили подключаются отдельным mixin и остаются внутри локальных корней вкладок.
         */
        for (String theme : supportedThemes()) {
            String scss = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/settings-window-sections.scss");
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");

            assertTrue("В теме " + theme + " отсутствует корневой namespace SettingsWindow",
                    scss.contains(".ext-settings-window"));
            assertTrue("В теме " + theme + " отсутствует дизайн вкладки «Интерфейс»",
                    scss.contains(".interface-settings-editor"));
            assertTrue("В теме " + theme + " отсутствует дизайн вкладки email",
                    scss.contains(".email-settings-editor"));
            assertTrue("В теме " + theme + " не импортирован локальный SCSS",
                    styles.contains("@import \"com.company.hunttech/settings-window-sections\";"));
            assertTrue("В теме " + theme + " не подключён mixin вкладок",
                    styles.contains("@include settings-window-sections;"));
            assertTrue("В теме " + theme + " отсутствует тёмная контекстная панель",
                    scss.contains("background-color: #172638"));
            assertTrue("В теме " + theme + " отсутствует акцент JobCandidateEdit",
                    scss.contains("color: #ffb11b"));
            assertTrue("В теме " + theme + " вкладки не получили высоту JobCandidateEdit",
                    scss.contains("height: 48px"));
            assertTrue("В теме " + theme + " поля не получили плотность JobCandidateEdit",
                    scss.contains("min-height: 38px"));
            assertFalse("В теме " + theme + " найден глобальный .v-table",
                    scss.contains("\n.v-table"));
            assertFalse("В теме " + theme + " найден глобальный .v-label",
                    scss.contains("\n.v-label"));
            assertFalse("В теме " + theme + " найден глобальный .v-button",
                    scss.contains("\n.v-button"));
            assertFalse("В теме " + theme + " найден глобальный .v-tabsheet",
                    scss.contains("\n.v-tabsheet"));
            assertFalse("В теме " + theme + " обнаружена зависимость от JobCandidateEdit",
                    scss.contains(".job-candidate-editor"));
        }
    }

    @Test
    public void settingsWindowVisualLayerUsesJobCandidatePrinciplesThroughOwnNamespace()
            throws IOException {
        /*
         * Визуальные токены JobCandidateEdit адаптируются, а не подключаются напрямую.
         * Проверка закрепляет отдельный namespace, контрастную боковую панель,
         * плотность полей и выраженную иерархию рабочей области.
         */
        String haloScss = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/settings-window-sections.scss");
        String concept = readProjectFile(
                "docs/architecture/HRM_HuntTech_UI_UX_Design_Concept.md");

        assertTrue(haloScss.contains(".ext-settings-window"));
        assertTrue(haloScss.contains("$v-app-background-color"));
        assertTrue(haloScss.contains("$v-panel-background-color"));
        assertTrue(haloScss.contains("$v-font-color"));
        assertTrue(haloScss.contains("$v-selection-color"));
        assertTrue(haloScss.contains("background-color: #172638"));
        assertTrue(haloScss.contains("linear-gradient(180deg, #172638"));
        assertTrue(haloScss.contains("color: #ffb11b"));
        assertTrue(haloScss.contains("box-shadow: 5px 0 20px"));
        assertTrue(haloScss.contains(".ext-settings-tabs"));
        assertTrue(haloScss.contains("height: 48px"));
        assertTrue(haloScss.contains("min-height: 58px"));
        assertTrue(haloScss.contains("min-height: 38px"));
        assertTrue(haloScss.contains(".v-filterselect-focus"));
        assertTrue(haloScss.contains(".v-disabled"));
        assertFalse(haloScss.contains(".job-candidate-editor"));

        assertTrue(concept.contains("JobCandidateEdit"));
        assertTrue(concept.contains(".ext-settings-window"));
        assertTrue(concept.contains("тёмная контекстная панель"));
        assertTrue(concept.contains("не обновляется автоматически"));
    }

    @Test
    public void databaseMigrationsContainPreferenceColumn() throws IOException {
        String updateSql = readProjectFile(
                "modules/core/db/update/postgres/26/260723-1-addPreferPersonalAiApiSettings.sql");
        String liquibase = readProjectFile(
                "modules/core/db/changelog/260723-1-addPreferPersonalAiApiSettings.xml");
        String liquibaseMaster = readProjectFile(
                "modules/core/db/changelog/db.changelog-master.xml");

        assertTrue(updateSql.contains("PREFER_PERSONAL_AI_API_SETTINGS"));
        assertTrue(updateSql.contains("DEFAULT FALSE"));
        assertTrue(liquibase.contains("PREFER_PERSONAL_AI_API_SETTINGS"));
        assertTrue(liquibase.contains("defaultValueBoolean=\"false\""));
        assertTrue(liquibaseMaster.contains("260723-1-addPreferPersonalAiApiSettings.xml"));
    }

    @Test
    public void localStartupAppliesDatabaseMigrationsBeforeDeploy() throws IOException {
        String startScript = readProjectFile("scripts/start-app.sh");

        // Регрессия защищает запуск экранов от новой entity-модели поверх устаревшей схемы PostgreSQL.
        int updateDbPosition = startScript.indexOf("./gradlew updateDb");
        int deployPosition = startScript.indexOf("./gradlew clean deploy");

        assertTrue("Локальный запуск обязан выполнять updateDb", updateDbPosition >= 0);
        assertTrue("updateDb должен выполняться до deploy", updateDbPosition < deployPosition);
    }

    private List<String> supportedThemes() {
        return Arrays.asList(
                "halo",
                "havana",
                "helium",
                "hover",
                "hunttech-modern",
                "hunttech-modern-light",
                "hunttech-modern-dark");
    }

    private int countOccurrences(String source, String fragment) {
        int count = 0;
        int start = 0;
        while ((start = source.indexOf(fragment, start)) >= 0) {
            count++;
            start += fragment.length();
        }
        return count;
    }

    private String readProjectFile(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
