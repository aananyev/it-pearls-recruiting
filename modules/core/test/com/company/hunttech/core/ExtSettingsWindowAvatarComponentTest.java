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
 * Защищает XML- и SCSS-контракт фотографии пользователя и левой панели вкладки «Обо мне».
 */
public class ExtSettingsWindowAvatarComponentTest {

    private static final String[] SUPPORTED_THEMES = {
            "halo",
            "havana",
            "helium",
            "hover",
            "hunttech-modern",
            "hunttech-modern-light",
            "hunttech-modern-dark"
    };

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

    @Test
    public void aboutMeSidebarFixIsScopedAndIncludedInAllThemes() throws IOException {
        for (String theme : SUPPORTED_THEMES) {
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            String sidebarFix = readProjectFile(
                    "modules/web/themes/" + theme
                            + "/com.company.hunttech/ext-settings-about-sidebar-fix.scss");

            assertTrue(theme + ": partial не импортирован",
                    styles.contains("@import \"com.company.hunttech/ext-settings-about-sidebar-fix\";"));
            assertTrue(theme + ": mixin не подключён",
                    styles.contains("@include ext-settings-about-sidebar-fix;"));

            // Регрессия со скриншота: панель не должна заходить под footer, а длинные captions — перекрываться.
            assertTrue(sidebarFix.contains("height: calc(100% - 18px) !important;"));
            assertTrue(sidebarFix.contains("overflow-y: auto !important;"));
            assertTrue(sidebarFix.contains("height: auto !important;"));
            assertTrue(sidebarFix.contains(".v-button-caption"));
            assertTrue(sidebarFix.contains("white-space: normal !important;"));

            // Более специфичное локальное правило не даёт старому border-radius: 8px сделать аватар квадратным.
            assertTrue(sidebarFix.contains(".ht-oval-image"));
            assertTrue(sidebarFix.contains("border-radius: 50% !important;"));
            assertTrue(sidebarFix.contains("clip-path: circle(50% at 50% 50%);"));

            // Исправление ограничено левой панелью и не переоформляет рабочие карточки справа.
            assertFalse(sidebarFix.contains(".user-ai-profile-content"));
            assertFalse(sidebarFix.contains(".user-ai-profile-toolbar"));
            assertFalse(sidebarFix.contains(".user-ai-profile-section"));
        }
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
