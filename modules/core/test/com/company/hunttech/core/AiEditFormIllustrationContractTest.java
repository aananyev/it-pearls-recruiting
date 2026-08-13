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
            assertTrue(entry.getKey(), xml.contains(
                    "fallbackThemePath=\"icons/ai/" + entry.getValue() + "\""));

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

    private Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return root;
    }
}
