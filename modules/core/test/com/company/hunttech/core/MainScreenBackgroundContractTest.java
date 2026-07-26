package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Закрепляет изоляцию персонального фона от существующей бизнес-логики экранов.
 * Тесты проверяют исходный контракт, не поднимая Vaadin UI и middleware.
 */
public class MainScreenBackgroundContractTest {

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
    public void generatedCatalogContainsTenVariantsForAllSevenThemes() throws IOException {
        String service = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/MainScreenBackgroundService.java");

        assertTrue(service.contains("VARIANT_COUNT = 10"));
        assertEquals("Каталог должен содержать ровно семь theme-aware палитр",
                THEMES.length, countOccurrences(service, "palettes.put("));
        for (String theme : THEMES) {
            assertTrue("Не найдена палитра темы " + theme,
                    service.contains("palettes.put(\"" + theme + "\""));
        }
        for (int variant = 0; variant < 10; variant++) {
            assertTrue("Не найден SVG-вариант " + variant,
                    service.contains("case " + variant + ":"));
        }

        // Регрессия защищает фактический формат, пригодный для CSS background.
        assertTrue(service.contains("image/svg+xml"));
        assertTrue(service.contains("viewBox=\\\"0 0 1920 1080\\\""));
        assertTrue(service.contains("preserveAspectRatio=\\\"xMidYMid slice\\\""));
        assertTrue(service.contains("\"hrm-main-\" + themeName + \"-\" + variant + \".svg\""));
    }

    @Test
    public void dynamicResourceIsRegisteredAfterVaadinConnectorAttach() throws IOException {
        String controller = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/HrmMainScreen.java");

        assertTrue(controller.contains("onAfterShowBackground(AfterShowEvent event)"));
        assertFalse(controller.contains("onBeforeShowBackground(BeforeShowEvent event)"));
        assertOrdered(controller,
                "new Image(null, resource)",
                "vaadinLayout.addComponent(backgroundResourceHolder)",
                "ResourceReference.create(",
                "backgroundResourceHolder, \"src\"");
        assertTrue(controller.contains("mainDashboard.unwrap(com.vaadin.ui.Component.class)"));
        assertTrue(controller.contains("vaadinDashboard.addStyleName(sessionStyle)"));
    }

    @Test
    public void customBackgroundHasPriorityOverRandomThemeCatalog() throws IOException {
        String service = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/MainScreenBackgroundService.java");

        assertOrdered(service,
                "FileDescriptor customBackground = loadUserBackground(currentUser)",
                "Optional<Resource> customResource = createCustomResource(customBackground)",
                "if (customResource.isPresent())",
                "ThreadLocalRandom.current().nextInt(VARIANT_COUNT)");
        assertTrue(service.contains("CUSTOM_BACKGROUND_PREFIX = \"hrm-main-background-\""));
    }

    @Test
    public void mainScreenExtensionPreservesExtMainScreenBusinessLogic() throws IOException {
        String controller = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/HrmMainScreen.java");
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/hrm-main-screen.xml");
        String properties = source("modules/web/src/com/company/hunttech/web-app.properties");

        assertTrue(controller.contains("class HrmMainScreen extends ExtMainScreen"));
        assertTrue(controller.contains("resolveForUser"));
        assertTrue(controller.contains("background-size:cover"));
        assertTrue(descriptor.contains("extends=\"/com/company/hunttech/web/screens/mainscreen/ext-main-screen.xml\""));
        assertTrue(properties.contains("cuba.web.mainScreenId=hrmMainScreen"));
        assertFalse(controller.contains("publishMyNotification"));
        assertFalse(controller.contains("checkPersonalReserveCandidates"));
    }

    @Test
    public void settingsExtensionUsesExistingUserSettingsFileWithoutEntityOrDatabaseChange() throws IOException {
        String controller = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowMainBackground.java");
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-main-background.xml");
        String userSettings = source("modules/global/src/com/company/hunttech/entity/UserSettings.java");

        assertTrue(controller.contains("class ExtSettingsWindowMainBackground extends ExtSettingsWindowInterfaceLayout"));
        assertTrue(controller.contains("UserSettings.fileImageFace"));
        assertFalse(descriptor.contains("datasource=\"userSettingsDs\""));
        assertFalse(descriptor.contains("property=\"fileImageFace\""));
        assertTrue(controller.contains("setFileImageFace(committedDescriptor)"));
        assertTrue(descriptor.contains("invoke=\"clearMainScreenBackground\""));
        assertTrue(userSettings.contains("private FileDescriptor fileImageFace;"));
        assertFalse(controller.contains("@Entity"));
        assertFalse(controller.contains("@Column"));
    }

    @Test
    public void settingsCardHasIsolatedLayoutAndThemeStyles() throws IOException {
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-main-background.xml");

        assertTrue(descriptor.contains("main-screen-background-card"));
        assertTrue(descriptor.contains("mainScreenBackgroundCustomOption"));
        assertTrue(descriptor.contains("mainScreenBackgroundDefaultOption"));
        assertTrue(descriptor.contains("caption=\"Использовать системные фоны\""));
        assertFalse(descriptor.contains("stylename=\"danger\""));

        for (String theme : THEMES) {
            String themeRoot = "modules/web/themes/" + theme + "/";
            String styles = source(themeRoot + "styles.scss");
            String localScss = source(themeRoot
                    + "com.company.hunttech/main-screen-background-settings.scss");

            assertTrue("SCSS не импортирован в теме " + theme,
                    styles.contains("@import \"com.company.hunttech/main-screen-background-settings\";"));
            assertTrue("SCSS mixin не подключён в теме " + theme,
                    styles.contains("@include main-screen-background-settings;"));
            assertTrue("Нет локального namespace в теме " + theme,
                    localScss.contains(".main-screen-background-card"));
            assertFalse("Запрещён глобальный селектор кнопки в теме " + theme,
                    localScss.contains("\n.v-button"));
        }
    }

    @Test
    public void clearActionReturnsToThemeRandomizationAndDeletesOnlyMarkedFiles() throws IOException {
        String controller = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowMainBackground.java");

        assertTrue(controller.contains("public void clearMainScreenBackground()"));
        assertTrue(controller.contains("currentBackground == null"));
        assertTrue(controller.contains("setFileImageFace(null)"));
        assertTrue(controller.contains("refreshBackgroundStatus()"));
        assertTrue(controller.contains("mainScreenBackgroundService.isCustomBackground(descriptor)"));
        assertTrue(controller.contains("!Objects.equals(descriptor.getId(), activeFileId)"));
    }

    private int countOccurrences(String source, String fragment) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(fragment, offset)) >= 0) {
            count++;
            offset += fragment.length();
        }
        return count;
    }

    private String source(String relativePath) throws IOException {
        Path path = projectRoot().resolve(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * Находит корень проекта независимо от рабочей директории Gradle test task.
     */
    private Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return root;
    }

    private void assertOrdered(String source, String... fragments) {
        int previous = -1;
        for (String fragment : fragments) {
            int current = source.indexOf(fragment);
            assertTrue("Не найден фрагмент: " + fragment, current >= 0);
            assertTrue("Нарушен порядок для: " + fragment, current > previous);
            previous = current;
        }
    }
}
