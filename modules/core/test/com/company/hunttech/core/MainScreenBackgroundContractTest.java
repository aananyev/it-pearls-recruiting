package com.company.hunttech.core;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Закрепляет архитектурный контракт фона до запуска screen-level и браузерной проверки.
 * Тест не подменяет фактический computed DOM style и HTTP-проверку theme-ресурса.
 */
public class MainScreenBackgroundContractTest {

    private static final String[] THEMES = {
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark"
    };

    @Test
    public void themeCatalogContainsTenRasterVariantsForAllSevenThemes() throws IOException {
        String service = source(
                "modules/web/src/com/company/hunttech/web/screens/mainscreen/MainScreenBackgroundService.java");

        // Регрессия запрещает возврат runtime-SVG: фон должен разрешаться только из каталога активной темы.
        assertTrue(service.contains("VARIANT_COUNT = 10"));
        assertTrue(service.contains("new ThemeResource(\"backgrounds/\" + (variant + 1) + \".jpg\")"));
        assertTrue(service.contains("SUPPORTED_THEMES"));
        assertFalse(service.contains("createSvg("));
        assertFalse(service.contains("createPalettes("));
        assertFalse(service.contains("class Palette"));
        assertFalse(service.contains("image/svg+xml"));

        for (String theme : THEMES) {
            assertTrue(service.contains("\"" + theme + "\""));
            Path backgroundDirectory = projectRoot()
                    .resolve("modules/web/themes")
                    .resolve(theme)
                    .resolve("backgrounds");
            assertTrue("Не найден каталог фонов темы " + theme,
                    Files.isDirectory(backgroundDirectory));

            try (Stream<Path> files = Files.list(backgroundDirectory)) {
                assertEquals("Тема " + theme + " должна содержать ровно 10 файлов",
                        10L, files.filter(Files::isRegularFile).count());
            }

            for (int variant = 1; variant <= 10; variant++) {
                Path background = backgroundDirectory.resolve(variant + ".jpg");
                assertTrue("Не найден JPG " + background, Files.isRegularFile(background));
                BufferedImage image = ImageIO.read(background.toFile());
                assertNotNull("Файл не распознан как растровое изображение: " + background, image);
                assertEquals("Неверная ширина фона " + background, 1920, image.getWidth());
                assertEquals("Неверная высота фона " + background, 1080, image.getHeight());
            }
        }
    }

    @Test
    public void annotatedMainScreenIsConfiguredConsistentlyAndNotRegisteredAsLegacyScreen()
            throws IOException {
        String moduleProperties = source("modules/web/src/com/company/hunttech/web-app.properties");
        String componentProperties = source("com/company/hunttech/web-app.properties");
        String screenConfig = source("modules/web/src/com/company/hunttech/web-screens.xml");
        String controller = source(
                "modules/web/src/com/company/hunttech/web/screens/mainscreen/HrmMainScreen.java");
        String descriptor = source(
                "modules/web/src/com/company/hunttech/web/screens/mainscreen/hrm-main-screen.xml");

        assertMainScreenId(moduleProperties, "web-модуль");
        assertMainScreenId(componentProperties, "app-component");
        assertFalse(screenConfig.contains("<screen id=\"hrmMainScreen\""));
        assertTrue(controller.contains("@UiController(\"hrmMainScreen\")"));
        assertTrue(controller.contains("@UiDescriptor(\"hrm-main-screen.xml\")"));
        assertTrue(controller.contains("class HrmMainScreen extends ExtMainScreen"));
        assertTrue(descriptor.contains(
                "extends=\"/com/company/hunttech/web/screens/mainscreen/ext-main-screen.xml\""));
    }

    @Test
    public void dedicatedLayerOwnsTheOnlyBackgroundImage() throws IOException {
        String controller = source(
                "modules/web/src/com/company/hunttech/web/screens/mainscreen/HrmMainScreen.java");

        // Фон применяется через CSS-инъекцию на mainVBox, не через отдельный слой
        assertTrue(controller.contains("vaadinVBox.addStyleName(currentBackgroundStyleName)"));
        assertTrue(controller.contains("vaadinDashboard.addStyleName(\"hrm-dashboard-transparent\")"));
        assertTrue(controller.contains("page.getStyles().add(css)"));
        assertTrue(controller.contains("background-image: url('"));
        assertTrue(controller.contains("background-size: cover"));
        assertTrue(controller.contains("background: transparent !important"));
        assertTrue(controller.contains("data-hrm-main-background"));
        assertFalse(controller.contains("private CssLayout mainScreenBackgroundLayer"));
    }

    @Test
    public void backgroundRefreshUsesUiEventAndAvoidsImmediateVariantRepeat()
            throws IOException {
        String controller = source(
                "modules/web/src/com/company/hunttech/web/screens/mainscreen/HrmMainScreen.java");
        String event = source(
                "modules/web/src/com/company/hunttech/web/screens/mainscreen/MainScreenBackgroundChangedEvent.java");
        String service = source(
                "modules/web/src/com/company/hunttech/web/screens/mainscreen/MainScreenBackgroundService.java");

        assertTrue(event.contains("extends ApplicationEvent implements UiEvent"));
        assertTrue(controller.contains("@EventListener"));
        assertTrue(controller.contains(
                "onMainScreenBackgroundChanged(MainScreenBackgroundChangedEvent event)"));
        assertTrue(service.contains("LAST_VARIANT_ATTRIBUTE"));
        assertTrue(service.contains("nextInt(VARIANT_COUNT - 1)"));
        assertTrue(service.contains("candidate >= previous ? candidate + 1 : candidate"));
        assertTrue(service.contains("setAttribute(attributeName, variant)"));
    }

    @Test
    public void mainScreenExtensionPreservesExtMainScreenBusinessLogic() throws IOException {
        String controller = source(
                "modules/web/src/com/company/hunttech/web/screens/mainscreen/HrmMainScreen.java");
        String baseController = source(
                "modules/web/src/com/company/hunttech/web/screens/mainscreen/ExtMainScreen.java");
        assertTrue(controller.contains("class HrmMainScreen extends ExtMainScreen"));
        assertTrue(baseController.contains("class ExtMainScreen extends MainScreen"));
        assertTrue(baseController.contains("onBeforeShow(BeforeShowEvent event)"));
        assertTrue(baseController.contains("onAfterShow1(AfterShowEvent event)"));
        assertFalse(controller.contains("publishMyNotification"));
        assertFalse(controller.contains("checkPersonalReserveCandidates"));
    }

    @Test
    public void uploadIsNormalizedBeforeDatasourceUpdate() throws IOException {
        String controller = source(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowMainBackground.java");
        String descriptor = source(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-main-background.xml");
        String processor = source(
                "modules/web/src/com/company/hunttech/web/screens/mainscreen/MainScreenBackgroundImageProcessor.java");

        assertTrue(descriptor.contains("uploadButtonCaption=\"Выбрать изображение\""));
        assertTrue(descriptor.contains("showFileName=\"true\""));
        assertTrue(descriptor.contains("height=\"36px\""));
        assertTrue(descriptor.contains("permittedExtensions=\".png,.jpg,.jpeg,.webp\""));
        assertTrue(descriptor.contains("fileSizeLimit=\"15728640\""));
        assertFalse(descriptor.contains("caption=\"Выбрать изображение\""));

        assertTrue(controller.contains("imageProcessor.process"));
        assertTrue(controller.contains("addFileUploadSucceedListener"));
        assertTrue(controller.contains("addFileUploadErrorListener"));
        assertTrue(controller.contains("imageProcessor.process"));
        assertTrue(controller.contains("createNormalizedDescriptor"));
        assertTrue(controller.contains("dataManager.commit(normalizedDescriptor"));
        assertTrue(controller.contains("setFileImageFace(committedDescriptor"));
        assertTrue(controller.contains("refreshBackgroundStatus()"));

        assertTrue(processor.contains("MAX_SOURCE_PIXELS = 40_000_000L"));
        assertTrue(processor.contains("MAX_OUTPUT_BYTES = 4L * 1024L * 1024L"));
        assertTrue(processor.contains("reader.getWidth(0)"));
        assertTrue(processor.contains("reader.getHeight(0)"));
        assertOrdered(processor,
                "validateDimensions(sourceWidth, sourceHeight)",
                "reader.read(0, readParam)");
        assertTrue(processor.contains("new IIOImage(image, null, null)"));
        assertTrue(processor.contains("return ImageFormat.WEBP"));
    }

    @Test
    public void settingsNavigationContainsMainScreenBackgroundSection() throws IOException {
        String controller = source(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowMainBackground.java");
        assertTrue(controller.contains("interfaceSettingsBackgroundNav"));
        assertTrue(controller.contains("setCaption(\"Фон главного экрана\")"));
        assertTrue(controller.contains("interfaceSettingsNavigation.add(interfaceSettingsBackgroundNav)"));
        assertTrue(controller.contains("public void selectInterfaceBackgroundSettings()"));
        assertTrue(controller.contains("mainScreenBackgroundUpload.focus()"));
    }

    @Test
    public void settingsOkCancelAndUiRefreshUseExplicitContract() throws IOException {
        String controller = source(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowMainBackground.java");
        assertOrdered(controller,
                "successfulCommitClosing = true",
                "super.commit()",
                "cleanupUnreferencedBackgrounds(settingsId)",
                "events.publish(new MainScreenBackgroundChangedEvent(this))");
        assertTrue(controller.contains("Используется случайный фон активной темы."));
        assertTrue(controller.contains("Используется пользовательский фон."));
        assertTrue(controller.contains("Остаться в экране или выйти без сохранения?"));
        assertTrue(controller.contains("withCaption(\"Остаться\")"));
        assertTrue(controller.contains("withCaption(\"Выйти без сохранения\")"));
        assertTrue(controller.contains("discardAndClose()"));
        String discard = between(controller,
                "private void discardAndClose()",
                "private void refreshBackgroundStatus()");
        assertTrue(discard.contains("pendingCreated"));
        assertTrue(discard.contains("closeWithDiscard()"));
        assertFalse(discard.contains("super.commit()"));
    }

    @Test
    public void webIntegrationInfrastructureIsEnabledWithoutRemovingJunit4()
            throws IOException {
        String build = source("build.gradle");
        String container = source(
                "modules/web/test/com/company/hunttech/web/HrmWebTestContainer.java");
        String integrationTest = source(
                "modules/web/test/com/company/hunttech/web/screens/mainscreen/HrmMainScreenIntegrationTest.java");

        assertTrue(build.contains("com.twelvemonkeys.imageio:imageio-webp:3.13.1"));
        assertTrue(build.contains("org.junit.jupiter:junit-jupiter-api:5.5.2"));
        assertTrue(build.contains("org.junit.vintage:junit-vintage-engine:5.5.2"));
        assertTrue(build.contains("useJUnitPlatform()"));
        assertTrue(container.contains("extends TestContainer"));
        assertTrue(container.contains("test-web-app.properties"));
        assertTrue(integrationTest.contains("TestUiEnvironment"));
        assertTrue(integrationTest.contains("screens.create(\"hrmMainScreen\", OpenMode.ROOT)"));
        assertTrue(integrationTest.contains("data-hrm-main-background"));
        assertTrue(integrationTest.contains("MainScreenBackgroundChangedEvent"));
    }

    @Test
    public void cleanupDeletesOnlyMarkedUnreferencedFiles() throws IOException {
        String controller = source(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowMainBackground.java");
        String service = source(
                "modules/web/src/com/company/hunttech/web/screens/mainscreen/MainScreenBackgroundService.java");

        assertTrue(service.contains("CUSTOM_BACKGROUND_PREFIX = \"hrm-main-background-\""));
        assertTrue(controller.contains("mainScreenBackgroundService.isCustomBackground(descriptor)"));
        assertTrue(controller.contains("!Objects.equals(descriptor.getId(), activeFileId)"));
        assertTrue(controller.contains("pendingCreated"));
        assertTrue(controller.contains("pendingRemoval"));
        String clear = between(controller,
                "public void clearMainScreenBackground()",
                "protected void commit()");
        assertFalse(clear.contains("dataManager.commit"));
        assertFalse(clear.contains("fileStorageService.removeFile"));
    }

    private void assertMainScreenId(String properties, String sourceName) {
        assertTrue(sourceName + " должен выбирать HrmMainScreen",
                properties.contains("cuba.web.mainScreenId=hrmMainScreen"));
        assertFalse(sourceName + " не должен возвращать legacy ExtMainScreen",
                properties.contains("cuba.web.mainScreenId=extMainScreen"));
    }

    private String source(String relativePath) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relativePath)),
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

    private String between(String source, String startFragment, String endFragment) {
        int start = source.indexOf(startFragment);
        int end = source.indexOf(endFragment, start + startFragment.length());
        assertTrue("Не найдено начало фрагмента: " + startFragment, start >= 0);
        assertTrue("Не найден конец фрагмента: " + endFragment, end > start);
        return source.substring(start, end);
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
