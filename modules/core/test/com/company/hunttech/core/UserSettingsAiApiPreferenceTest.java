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
    public void allSupportedThemesContainLocalizedAiSettingsStyles() throws IOException {
        List<String> themes = Arrays.asList(
                "halo",
                "havana",
                "helium",
                "hover",
                "hunttech-modern",
                "hunttech-modern-light",
                "hunttech-modern-dark");

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

    private String readProjectFile(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
