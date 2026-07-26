package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Закрепляет изоляцию персонального фона от существующей бизнес-логики экранов.
 * Тесты проверяют исходный контракт, не поднимая Vaadin UI и middleware.
 */
public class MainScreenBackgroundContractTest {

    @Test
    public void generatedCatalogContainsTenVariantsForAllSevenThemes() throws IOException {
        String service = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/MainScreenBackgroundService.java");

        assertTrue(service.contains("VARIANT_COUNT = 10"));
        assertTrue(service.contains("palettes.put(\"halo\""));
        assertTrue(service.contains("palettes.put(\"havana\""));
        assertTrue(service.contains("palettes.put(\"helium\""));
        assertTrue(service.contains("palettes.put(\"hover\""));
        assertTrue(service.contains("palettes.put(\"hunttech-modern\""));
        assertTrue(service.contains("palettes.put(\"hunttech-modern-light\""));
        assertTrue(service.contains("palettes.put(\"hunttech-modern-dark\""));
        for (int variant = 0; variant < 10; variant++) {
            assertTrue(service.contains("case " + variant + ":") || variant == 9 && service.contains("case 9:"));
        }
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
    public void clearActionReturnsToThemeRandomizationAndDeletesOnlyMarkedFiles() throws IOException {
        String controller = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowMainBackground.java");

        assertOrdered(controller,
                "public void clearMainScreenBackground()",
                "currentBackground == null",
                "setFileImageFace(null)",
                "refreshBackgroundStatus()");
        assertTrue(controller.contains("mainScreenBackgroundService.isCustomBackground(descriptor)"));
        assertTrue(controller.contains("!Objects.equals(descriptor.getId(), activeFileId)"));
    }

    private String source(String relativePath) throws IOException {
        Path path = Paths.get(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
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
