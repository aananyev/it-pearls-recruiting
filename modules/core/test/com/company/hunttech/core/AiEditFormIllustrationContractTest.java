package com.company.hunttech.core;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Контракт тематических sidebar-иллюстраций AI Edit-форм HRM HuntTech.
 *
 * Защищает отдельный semantic fallback для каждой формы, размер исходного asset 200×200,
 * наличие ресурса во всех поддерживаемых темах и сохранение отображения 176×176,
 * уже выровненного по эталону JobCandidateEdit.
 */
public class AiEditFormIllustrationContractTest {

    private static final String[] THEMES = {
            "halo",
            "havana",
            "helium",
            "hover",
            "hunttech-modern",
            "hunttech-modern-dark",
            "hunttech-modern-light"
    };

    @Test
    public void aiEditFormsUseDedicatedHuntTechIllustrations() throws IOException {
        Map<String, String> screenAssets = new LinkedHashMap<>();
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/adminaiconfiguration/admin-ai-configuration-edit.xml",
                "admin-ai-configuration.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/aifunctionconfiguration/ai-function-configuration-edit.xml",
                "ai-function-configuration.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/user-ai-configuration-edit.xml",
                "user-ai-configuration.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/useraifunctionoverride/user-ai-function-override-edit.xml",
                "user-ai-function-override.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/vacancyprompttemplate/vacancy-prompt-template-edit.xml",
                "vacancy-prompt-template.png");

        for (Map.Entry<String, String> entry : screenAssets.entrySet()) {
            String xml = new String(Files.readAllBytes(projectRoot().resolve(entry.getKey())), StandardCharsets.UTF_8);

            assertTrue(entry.getKey(), xml.contains("width=\"176px\""));
            assertTrue(entry.getKey(), xml.contains("height=\"176px\""));
            assertTrue(entry.getKey(), xml.contains("ovalWidth=\"176px\""));
            assertTrue(entry.getKey(), xml.contains("ovalHeight=\"176px\""));
            // Иллюстрация подаётся как прямой theme-ресурс в ovalImage (без fallback-механики).
            assertTrue(entry.getKey(), xml.contains("<ovalImage id=\""));
            assertTrue(entry.getKey(), xml.contains(
                    "<theme path=\"icons/ai/" + entry.getValue() + "\"/>"));

            for (String theme : THEMES) {
                Path asset = projectRoot().resolve(
                        "modules/web/themes/" + theme + "/icons/ai/" + entry.getValue());
                assertTrue("Не найден theme asset: " + asset, Files.isRegularFile(asset));

                BufferedImage image = ImageIO.read(asset.toFile());
                assertNotNull("Не удалось прочитать PNG: " + asset, image);
                assertEquals("Ширина theme asset должна быть 200px: " + asset, 200, image.getWidth());
                assertEquals("Высота theme asset должна быть 200px: " + asset, 200, image.getHeight());
            }
        }
    }

    @Test
    public void staticFallbackWithoutDataBindingIsRendered() throws IOException {
        // OvaFallbackImage без dataContainer/property (статичная иллюстрация
        // AI Control Plane форм) обязан показывать fallbackThemePath, а не
        // пустой овал. Два уровня защиты:
        // 1) setFallbackThemePath применяет fallback сразу при статичном
        //    использовании (updateComponent у компонента без valueSource
        //    не вызывается вообще);
        // 2) tryApplyFallback дополнительно применяет fallback при
        //    valueSource == null, не затирая явный source контроллера.
        String delegate = new String(Files.readAllBytes(projectRoot().resolve(
                "modules/web/src/com/hunttech/hrm/web/components/delegate/"
                        + "FallbackImageResourceDelegate.java")), StandardCharsets.UTF_8);

        // Ветка немедленного применения в setFallbackThemePath
        assertTrue(delegate.contains("if (host.getBoundValueSource() == null && host.getSource() == null) {"));
        // Guard в tryApplyFallback
        assertTrue(delegate.contains("if (fallbackResource == null) {"));
        assertTrue(delegate.contains("if (valueSource == null) {"));
        assertTrue(delegate.contains("if (host.getSource() == null) {"));
        assertTrue(delegate.contains("host.updateValue(fallbackResource);"));
        assertTrue(delegate.contains("return true;"));
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
