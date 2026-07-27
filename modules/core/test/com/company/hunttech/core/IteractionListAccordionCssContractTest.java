package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает финальный локальный CSS-контракт IteractionListEdit:
 * компактные аккордеоны, читаемые частые действия и единый footer.
 */
public class IteractionListAccordionCssContractTest {
    private static final List<String> THEMES = Arrays.asList(
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark");

    @Test
    public void allThemesImportFinalScopedOverrideAfterReferences() throws IOException {
        for (String theme : THEMES) {
            String scss = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/iteraction-list-reference-finish.scss");
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");

            assertTrue(scss.contains(".iteraction-list-editor"));
            assertTrue(scss.contains(".iteraction-list-accordion-section"));
            assertTrue(scss.contains(".iteraction-list-popular-host"));
            assertTrue(scss.contains(".iteraction-list-footer-actions"));
            assertTrue(scss.contains("height: 40px !important;"));
            assertTrue(scss.contains("border-radius: 8px !important;"));
            assertTrue(scss.contains("visibility: visible !important;"));
            assertFalse(scss.contains("background: #2e7d32 !important;"));
            assertFalse(scss.contains("border-radius: 999px !important;"));
            assertFalse(scss.contains("@mixin iteraction-list-reference-finish-theme {\n  .v-panel"));
            assertTrue(styles.indexOf("@import \"com.company.hunttech/iteraction-list-reference-finish\";")
                    > styles.indexOf("@import \"com.company.hunttech/candidate-cv-editor\";"));
            assertTrue(styles.indexOf("@include iteraction-list-reference-finish-theme;")
                    > styles.indexOf("@include candidate-cv-editor-theme;"));
        }
    }

    @Test
    public void finalOverrideUsesCanonicalAccordionAndButtonGeometry() throws IOException {
        String scss = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/iteraction-list-reference-finish.scss");

        assertTrue(scss.contains("height: 48px;"));
        assertTrue(scss.contains("padding: 0 20px;"));
        assertTrue(scss.contains("border: 1px solid rgba($v-font-color, 0.15) !important;"));
        assertTrue(scss.contains("border-radius: 8px !important;"));
        assertTrue(scss.contains("min-height: 44px;"));
        assertTrue(scss.contains("padding: 9px 16px;"));
        assertTrue(scss.contains("font-size: 15px !important;"));
        assertTrue(scss.contains("font-weight: 600 !important;"));
        assertTrue(scss.contains(".iteraction-list-popular-button .v-button-wrap"));
        assertTrue(scss.contains("display: flex !important;"));
        assertTrue(scss.contains("object-fit: contain !important;"));
    }

    private String readProjectFile(String relativePath) throws IOException {
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
}
