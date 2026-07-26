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
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark"
    };

    @Test
    public void generatedCatalogContainsTenVariantsForAllSevenThemes() throws IOException {
        String service = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/MainScreenBackgroundService.java");
        assertTrue(service.contains("VARIANT_COUNT = 10"));
        assertEquals(THEMES.length, countOccurrences(service, "palettes.put("));
        for (String theme : THEMES) {
            assertTrue(service.contains("palettes.put(\"" + theme + "\""));
        }
        for (int variant = 0; variant < 10; variant++) {
            assertTrue(service.contains("case " + variant + ":"));
        }
        assertTrue(service.contains("image/svg+xml"));
        assertTrue(service.contains("viewBox=\\\"0 0 1920 1080\\\""));
        assertTrue(service.contains("preserveAspectRatio=\\\"xMidYMid slice\\\""));
    }

    @Test
    public void mainScreenIdIsExplicitlyRegistered() throws IOException {
        String properties = source("modules/web/src/com/company/hunttech/web-app.properties");
        String screenConfig = source("modules/web/src/com/company/hunttech/web-screens.xml");
        String controller = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/HrmMainScreen.java");
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/hrm-main-screen.xml");
        assertTrue(properties.contains("cuba.web.mainScreenId=hrmMainScreen"));
        assertTrue(screenConfig.contains("<screen id=\"hrmMainScreen\""));
        assertTrue(screenConfig.contains("/mainscreen/hrm-main-screen.xml"));
        assertTrue(controller.contains("@UiController(\"hrmMainScreen\")"));
        assertTrue(controller.contains("class HrmMainScreen extends ExtMainScreen"));
        assertTrue(descriptor.contains("extends=\"/com/company/hunttech/web/screens/mainscreen/ext-main-screen.xml\""));
    }

    @Test
    public void dynamicResourceIsRegisteredAfterVaadinConnectorAttach() throws IOException {
        String controller = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/HrmMainScreen.java");
        assertTrue(controller.contains("onAfterShowBackground(AfterShowEvent event)"));
        assertTrue(controller.contains("UI currentUi = UI.getCurrent()"));
        assertTrue(controller.contains("Page page = currentUi.getPage()"));
        assertFalse(controller.contains("Page.getCurrent()"));
        assertOrdered(controller,
                "new Image(null, resource)",
                "vaadinLayout.addComponent(backgroundResourceHolder)",
                "ensureAttachedToCurrentUi(currentUi, backgroundResourceHolder",
                "ResourceReference.create(",
                "backgroundResourceHolder, \"src\"");
        assertTrue(controller.contains("ensureAttachedToCurrentUi(currentUi, vaadinDashboard"));
        assertTrue(controller.contains("vaadinDashboard.addStyleName(sessionStyle)"));
        assertTrue(controller.contains("backgroundResourceHolder.setWidth(0, Unit.PIXELS)"));
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
        String baseController = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/ExtMainScreen.java");
        assertTrue(controller.contains("class HrmMainScreen extends ExtMainScreen"));
        assertTrue(baseController.contains("class ExtMainScreen extends MainScreen"));
        assertTrue(baseController.contains("onBeforeShow(BeforeShowEvent event)"));
        assertTrue(baseController.contains("onAfterShow1(AfterShowEvent event)"));
        assertFalse(controller.contains("publishMyNotification"));
        assertFalse(controller.contains("checkPersonalReserveCandidates"));
    }

    @Test
    public void settingsExtensionUsesExistingUserSettingsFileWithoutEntityOrDatabaseChange() throws IOException {
        String controller = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowMainBackground.java");
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-main-background.xml");
        String userSettings = source("modules/global/src/com/company/hunttech/entity/UserSettings.java");
        assertTrue(controller.contains("UserSettings.fileImageFace"));
        assertFalse(descriptor.contains("datasource=\"userSettingsDs\""));
        assertTrue(controller.contains("setFileImageFace(committedDescriptor)"));
        assertTrue(userSettings.contains("private FileDescriptor fileImageFace;"));
        assertFalse(controller.contains("@Entity"));
    }

    @Test
    public void settingsNavigationContainsMainScreenBackgroundSection() throws IOException {
        String controller = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowMainBackground.java");
        assertTrue(controller.contains("interfaceSettingsBackgroundNav"));
        assertTrue(controller.contains("setCaption(\"Фон главного экрана\")"));
        assertTrue(controller.contains("interfaceSettingsNavigation.add(interfaceSettingsBackgroundNav)"));
        assertTrue(controller.contains("public void selectInterfaceBackgroundSettings()"));
        assertTrue(controller.contains("mainScreenBackgroundUpload.focus()"));
    }

    @Test
    public void settingsOkAndCancelUseExplicitCloseContract() throws IOException {
        String controller = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowMainBackground.java");
        assertOrdered(controller,
                "successfulCommitClosing = true",
                "super.commit()",
                "successfulCommitClosing = false");
        assertTrue(controller.contains("public boolean hasUnsavedChanges()"));
        assertTrue(controller.contains("return !successfulCommitClosing && super.hasUnsavedChanges()"));
        assertTrue(controller.contains("Остаться в экране или выйти без сохранения?"));
        assertTrue(controller.contains("withCaption(\"Остаться\")"));
        assertTrue(controller.contains("withCaption(\"Выйти без сохранения\")"));
        assertTrue(controller.contains("closeWithDiscard()"));
    }

    @Test
    public void settingsCardHasIsolatedLayoutAndThemeStyles() throws IOException {
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-main-background.xml");
        assertTrue(descriptor.contains("main-screen-background-card"));
        assertTrue(descriptor.contains("caption=\"Использовать системные фоны\""));
        assertFalse(descriptor.contains("stylename=\"danger\""));
        for (String theme : THEMES) {
            String themeRoot = "modules/web/themes/" + theme + "/";
            String styles = source(themeRoot + "styles.scss");
            String localScss = source(themeRoot + "com.company.hunttech/main-screen-background-settings.scss");
            assertTrue(styles.contains("@import \"com.company.hunttech/main-screen-background-settings\";"));
            assertTrue(styles.contains("@include main-screen-background-settings;"));
            assertTrue(localScss.contains(".main-screen-background-card"));
        }
    }

    @Test
    public void clearActionReturnsToThemeRandomizationAndDeletesOnlyMarkedFiles() throws IOException {
        String controller = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowMainBackground.java");
        assertTrue(controller.contains("public void clearMainScreenBackground()"));
        assertTrue(controller.contains("setFileImageFace(null)"));
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
        return new String(Files.readAllBytes(projectRoot().resolve(relativePath)), StandardCharsets.UTF_8);
    }

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
