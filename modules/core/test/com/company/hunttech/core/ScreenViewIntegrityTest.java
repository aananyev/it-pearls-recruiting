package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.entity.UserAiProfile;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.ViewRepository;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ScreenViewIntegrityTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    @Test
    public void test1_userAiProfile_class_exists() throws Exception {
        Class.forName("com.company.hunttech.entity.UserAiProfile");
    }

    @Test
    public void test2_userAiProfile_entity_registered() {
        Metadata metadata = AppBeans.get(Metadata.class);
        assertNotNull(metadata.getClassNN("hunttech_UserAiProfile"));
    }

    @Test
    public void test3_hunttech_service_bean() {
        assertNotNull(AppBeans.get("hunttech_UserAiContextService"));
    }

    @Test
    public void test4_hunttech_service_interface() {
        assertNotNull(AppBeans.get(com.company.hunttech.service.UserAiContextService.class));
    }

    @Test
    public void test5_userAiProfile_view_registered() throws IOException {
        /*
         * SettingsWindow создаёт legacy datasource до вызова контроллера. Проверяем
         * одновременно runtime ViewRepository и рабочие конфигурации обоих блоков,
         * чтобы отдельный view-файл не остался зарегистрирован только как app component.
         */
        ViewRepository viewRepository = AppBeans.get(ViewRepository.class);
        Metadata metadata = AppBeans.get(Metadata.class);
        assertNotNull(viewRepository.getView(metadata.getClassNN(UserAiProfile.class), "userAiProfile-view"));

        String coreProperties = readProjectFile("modules/core/src/com/company/hunttech/app.properties");
        String webProperties = readProjectFile("modules/web/src/com/company/hunttech/web-app.properties");
        String settingsXml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml");

        assertTrue(coreProperties.contains("com/company/hunttech/user-ai-profile-views.xml"));
        assertTrue(webProperties.contains("com/company/hunttech/user-ai-profile-views.xml"));
        assertTrue(settingsXml.contains("view=\"userAiProfile-view\""));
    }

    @Test
    public void test6_extUser_entity_registered() {
        Metadata metadata = AppBeans.get(Metadata.class);
        assertNotNull(metadata.getClassNN("hunttech_ExtUser"));
    }

    @Test
    public void test7_jobCandidate_entity_registered() {
        Metadata metadata = AppBeans.get(Metadata.class);
        assertNotNull(metadata.getClassNN("hunttech_JobCandidate"));
    }

    @Test
    public void test8_hunttech_model_root_registered() {
        Metadata metadata = AppBeans.get(Metadata.class);
        assertNotNull(metadata.getClassNN("hunttech_ExtUser"));
        assertNotNull(metadata.getClassNN("hunttech_JobCandidate"));
        assertNotNull(metadata.getClassNN("hunttech_UserAiProfile"));
    }

    private String readProjectFile(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта для проверки конфигурации SettingsWindow", root);
        return new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
