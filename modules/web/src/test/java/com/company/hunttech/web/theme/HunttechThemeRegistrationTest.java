package com.company.hunttech.web.theme;

import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HunttechThemeRegistrationTest {

    private static final Path WEB_MODULE = Paths.get("modules/web");
    private static final Path WEB_APP_PROPERTIES = WEB_MODULE.resolve(
            "src/com/company/hunttech/web-app.properties");
    private static final Set<String> HUNTTECH_THEMES = new LinkedHashSet<>(Arrays.asList(
            "hunttech-modern-light",
            "hunttech-modern-dark"));

    @Test
    public void modernThemesAreRegisteredForCubaSettingsWindow() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(WEB_APP_PROPERTIES)) {
            properties.load(input);
        }

        String themeConfig = properties.getProperty("cuba.themeConfig", "");
        Set<String> configuredFiles = new LinkedHashSet<>(Arrays.asList(themeConfig.trim().split("\\s+")));

        for (String theme : HUNTTECH_THEMES) {
            String constantsResource = "/com/company/hunttech/" + theme + "-theme.properties";
            assertTrue(theme + " is missing from cuba.themeConfig",
                    configuredFiles.contains(constantsResource));
            assertTrue(theme + " constants file is missing",
                    Files.isRegularFile(WEB_MODULE.resolve("src").resolve(constantsResource.substring(1))));
            assertTrue(theme + " compiled-theme source is missing",
                    Files.isRegularFile(WEB_MODULE.resolve("themes").resolve(theme).resolve("styles.scss")));
        }

        assertFalse("cuba.web.themeList is not used by CUBA SetupWindow",
                properties.containsKey("cuba.web.themeList"));
    }
}
