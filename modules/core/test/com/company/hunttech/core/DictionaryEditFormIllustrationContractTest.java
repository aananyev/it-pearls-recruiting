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
 * Контракт тематических sidebar-иллюстраций Edit-форм «Справочников» HRM HuntTech.
 *
 * Защищает: каждая форма справочника без загрузки изображения показывает
 * статичный ovalImage 176×176 (эталон JobCandidateEdit) с прямым theme-ресурсом
 * icons/dictionaries/{form}.png, размер исходного asset 200×200 и наличие ресурса
 * во всех поддерживаемых темах.
 */
public class DictionaryEditFormIllustrationContractTest {

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
    public void dictionaryEditFormsUseDedicatedStaticIllustrations() throws IOException {
        Map<String, String> screenAssets = new LinkedHashMap<>();
        // Формы без загрузки изображения: статичный ovalImage + theme path.
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/filetype/file-type-edit.xml",
                "file-type.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/grade/grade-edit.xml",
                "grade.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/currency/currency-edit.xml",
                "currency.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/outstaffingrates/outstaffing-rates-edit.xml",
                "outstaffing-rates.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/employeeworkstatus/employee-work-status-edit.xml",
                "employee-work-status.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/signicons/sign-icons-edit.xml",
                "sign-icons.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/country/country-edit.xml",
                "country.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/city/city-edit.xml",
                "city.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/region/region-edit.xml",
                "region.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/specialisation/specialisation-edit.xml",
                "specialisation.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/ownershup/ownershup-edit.xml",
                "ownershup.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/position/position-edit.xml",
                "position.png");
        screenAssets.put(
                "modules/web/src/com/company/hunttech/web/screens/iteraction/iteraction-edit.xml",
                "iteraction.png");

        for (Map.Entry<String, String> entry : screenAssets.entrySet()) {
            String xml = new String(Files.readAllBytes(projectRoot().resolve(entry.getKey())), StandardCharsets.UTF_8);

            assertTrue(entry.getKey(), xml.contains("width=\"176px\""));
            assertTrue(entry.getKey(), xml.contains("height=\"176px\""));
            assertTrue(entry.getKey(), xml.contains("ovalWidth=\"176px\""));
            assertTrue(entry.getKey(), xml.contains("ovalHeight=\"176px\""));
            // Иллюстрация подаётся как прямой theme-ресурс в ovalImage (без fallback-механики).
            assertTrue(entry.getKey(), xml.contains("<ovalImage id=\""));
            assertTrue(entry.getKey(), xml.contains(
                    "<theme path=\"icons/dictionaries/" + entry.getValue() + "\"/>"));

            for (String theme : THEMES) {
                Path asset = projectRoot().resolve(
                        "modules/web/themes/" + theme + "/icons/dictionaries/" + entry.getValue());
                assertTrue("Не найден theme asset: " + asset, Files.isRegularFile(asset));

                BufferedImage image = ImageIO.read(asset.toFile());
                assertNotNull("Не удалось прочитать PNG: " + asset, image);
                assertEquals("Ширина theme asset должна быть 200px: " + asset, 200, image.getWidth());
                assertEquals("Высота theme asset должна быть 200px: " + asset, 200, image.getHeight());
            }
        }
    }

    @Test
    public void dictionaryEditFormsWithUploadKeepOvaFallbackImage() throws IOException {
        // Формы с загрузкой изображения (SkillTree, SocialNetworkType) сохраняют
        // ovaFallbackImage с привязкой к данным — их логотип грузится пользователем.
        String skillTree = new String(Files.readAllBytes(projectRoot().resolve(
                "modules/web/src/com/company/hunttech/web/screens/skilltree/skill-tree-edit.xml")), StandardCharsets.UTF_8);
        String socialNetwork = new String(Files.readAllBytes(projectRoot().resolve(
                "modules/web/src/com/company/hunttech/web/screens/socialnetworktype/social-network-type-edit.xml")), StandardCharsets.UTF_8);

        assertTrue(skillTree.contains("<ovaFallbackImage id=\""));
        assertTrue(skillTree.contains("width=\"176px\""));
        assertTrue(socialNetwork.contains("<ovaFallbackImage id=\""));
        assertTrue(socialNetwork.contains("width=\"176px\""));
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
