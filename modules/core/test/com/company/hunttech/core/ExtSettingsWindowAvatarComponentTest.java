package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает XML-контракт фотографии пользователя во вкладке «Обо мне».
 */
public class ExtSettingsWindowAvatarComponentTest {

    @Test
    public void aboutMeUsesOvaFallbackImageForUserPhoto() throws IOException {
        String screenXml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml");

        assertTrue(screenXml.contains("<ovaFallbackImage id=\"userPic\""));
        assertTrue(screenXml.contains("width=\"176px\""));
        assertTrue(screenXml.contains("height=\"176px\""));
        assertTrue(screenXml.contains("ovalWidth=\"176px\""));
        assertTrue(screenXml.contains("ovalHeight=\"176px\""));
        assertTrue(screenXml.contains("fallbackThemePath=\"icons/no-programmer.jpeg\""));
        assertTrue(screenXml.contains("scaleMode=\"SCALE_DOWN\""));
        assertFalse(screenXml.contains("<image id=\"userPic\""));
    }

    @Test
    public void avatarReplacementPreservesControllerAndEntityContracts() throws IOException {
        String screenXml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml");
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindow.java");

        /*
         * OvaFallbackImage наследует Image, поэтому контроллер продолжает использовать
         * базовый тип и прежние component ID без функциональных изменений.
         */
        assertTrue(controller.contains("@Inject private Image userPic;"));
        assertTrue(controller.contains("@Inject private Image defaultPic;"));
        assertTrue(controller.contains("FileDescriptorImageHelper.setUserProfilePhoto(userPic"));
        assertTrue(screenXml.contains("<image id=\"defaultPic\""));
        assertTrue(screenXml.contains("id=\"userAvatarUpload\""));
        assertTrue(screenXml.contains("datasource=\"extUserDs\""));
        assertTrue(screenXml.contains("property=\"userAvatar\""));
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
