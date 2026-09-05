package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Защищает пользовательские темы HRM HuntTech от повторной потери базового
 * визуального слоя Halo и от сведения тёмной палитры к копии светлой.
 */
public class ModernThemesFoundationContractTest {

    private static final String LIGHT = "hunttech-modern-light";
    private static final String DARK = "hunttech-modern-dark";

    @Test
    public void themeSelectorUsesHrmHuntTechBranding() throws IOException {
        String themeConfig = source(
                "modules/web/src/com/company/hunttech/web/theme-config.xml");

        assertTrue(themeConfig.contains(
                "id=\"hunttech-modern-light\" name=\"HRM HuntTech Modern (Светлая)\""));
        assertTrue(themeConfig.contains(
                "id=\"hunttech-modern-dark\" name=\"HRM HuntTech Modern (Тёмная)\""));
    }

    @Test
    public void documentationFixesTheFoundationContract() throws IOException {
        String contract = source(
                "docs/architecture/HRM_HuntTech_Modern_Themes_Contract.md");
        String index = source("docs/architecture/README.md");

        assertTrue(contract.contains("## Назначение и бизнес-смысл (What & Why)"));
        assertTrue(contract.contains("## UI Context & Navigation"));
        assertTrue(contract.contains("## Behavior Summary"));
        assertTrue(contract.contains("@import \"../halo/halo\""));
        assertTrue(contract.contains("@include halo"));
        assertTrue(contract.contains("## История изменений"));

        assertTrue(index.contains("HRM_HuntTech_Modern_Themes_Contract.md"));
        assertTrue(index.contains("2026-08-04"));
    }

    private void assertThemeComposition(String theme, String extensionMixin)
            throws IOException {
        String styles = themeSource(theme, "styles.scss");

        int defaultsImport = styles.indexOf("@import \"" + theme + "-defaults\";");
        int haloImport = styles.indexOf("@import \"../halo/halo\";");
        int appComponentsImport = styles.indexOf("@import \"app-components\";");
        int haloInclude = styles.indexOf("@include halo;");
        int appComponentsInclude = styles.indexOf("@include app_components;");
        int extensionInclude = styles.indexOf("@include " + extensionMixin + ";");

        assertTrue("Defaults должны импортироваться первыми", defaultsImport >= 0);
        assertTrue("Halo должен импортироваться после defaults",
                haloImport > defaultsImport);
        assertTrue("app-components должен импортироваться после Halo",
                appComponentsImport > haloImport);
        assertTrue("Halo должен включаться внутри root темы", haloInclude >= 0);
        assertTrue("app_components должен включаться после Halo",
                appComponentsInclude > haloInclude);
        assertTrue("Theme extension должен включаться после app_components",
                extensionInclude > appComponentsInclude);

        // Тема не зависит от внешней доступности Google Fonts.
        assertFalse(styles.contains("fonts.googleapis.com"));
    }

    private String assertModernComponentLayer(String theme)
            throws IOException {
        String extension = themeSource(
                theme,
                "com.company.hunttech/modern-theme-components.scss");
        String styles = themeSource(theme, "styles.scss");

        assertTrue(extension.contains("@mixin modern-theme-components"));
        assertTrue(extension.contains(".c-app-menubar"));
        assertTrue(extension.contains(".v-textfield"));
        assertTrue(extension.contains(".v-button-primary"));
        assertTrue(extension.contains(".v-table"));
        assertTrue(extension.contains(".v-grid"));
        assertTrue(extension.contains(".v-tabsheet-tabitem-selected"));
        assertTrue(extension.contains(".v-window"));
        assertTrue(extension.contains(".v-Notification"));
        assertTrue(extension.contains(".ht-oval-image"));
        assertTrue(extension.contains("max-height: 32px"));
        assertTrue(extension.contains("$ht-focus-ring"));

        assertTrue(styles.contains(
                "@import \"com.company.hunttech/modern-theme-components\";"));
        assertTrue(styles.contains("@include modern-theme-components;"));
        return extension;
    }

    private String themeSource(String theme, String relativePath)
            throws IOException {
        return source("modules/web/themes/" + theme + "/" + relativePath);
    }

    private String source(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8);
    }

    private Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Не найден корень проекта HRM HuntTech");
    }
}
