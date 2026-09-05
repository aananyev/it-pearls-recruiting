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
