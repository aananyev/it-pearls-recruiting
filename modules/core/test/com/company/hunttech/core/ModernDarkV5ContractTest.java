package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Visual contract тест варианта 5 (современный тёмный) темы hunttech-modern-dark:
 * DataGrid, Table, TreeTable, TextArea, RichTextArea.
 *
 * Проверяет цепочку импорта (styles.scss → component partial), референсную
 * тёмную палитру, состояния, базовый слой темы и изоляцию других тем.
 */
public class ModernDarkV5ContractTest {

    private static final String THEME = "modules/web/themes/hunttech-modern-dark";
    private static final String SCSS = THEME + "/com.company.hunttech/modern-dark-component-style-v5.scss";
    private static final String STYLES = THEME + "/styles.scss";
    private static final String DEFAULTS = THEME + "/hunttech-modern-dark-defaults.scss";

    private static final List<String> OTHER_THEMES = Arrays.asList(
            "modules/web/themes/halo",
            "modules/web/themes/havana",
            "modules/web/themes/helium",
            "modules/web/themes/hover",
            "modules/web/themes/hunttech-modern",
            "modules/web/themes/hunttech-modern-light"
    );

    @Test
    public void scssConnectedInsideRootSelector() throws IOException {
        String styles = read(STYLES);
        assertTrue("modern-dark-component-style-v5 не подключён в styles.scss",
                styles.contains("@import \"com.company.hunttech/modern-dark-component-style-v5\";"));
        int impIdx = styles.indexOf("@import \"com.company.hunttech/modern-dark-component-style-v5\";");
        int openIdx = styles.indexOf(".hunttech-modern-dark {");
        int closeIdx = styles.lastIndexOf("}");
        assertTrue("Импорт не внутри корневого селектора .hunttech-modern-dark",
                impIdx > openIdx && impIdx < closeIdx);
    }

    @Test
    public void baseLayerRestored() throws IOException {
        String styles = read(STYLES);
        assertTrue("Нет @import \"../halo/halo\" (базовый слой)", styles.contains("@import \"../halo/halo\";"));
        assertTrue("Нет @include halo", styles.contains("@include halo;"));
        assertTrue("Нет @include app_components", styles.contains("@include app_components;"));
    }

    @Test
    public void allFiveComponentsPresent() throws IOException {
        String scss = read(SCSS);
        for (String sel : Arrays.asList(".v-grid-header", ".v-table-header-wrap",
                ".v-treegrid-header", ".v-tree", ".v-textarea", ".v-richtextarea",
                ".gwt-RichTextToolbar")) {
            assertTrue("Отсутствует компонент " + sel, scss.contains(sel));
        }
    }

    @Test
    public void requiredStatesPresent() throws IOException {
        String scss = read(SCSS);
        assertTrue("Нет hover", scss.contains(":hover"));
        assertTrue("Нет selected", scss.contains("-selected"));
        assertTrue("Нет focus", scss.contains("focus"));
        assertTrue("Нет disabled", scss.contains(".v-disabled"));
        assertTrue("Нет read-only", scss.contains(".v-readonly") || scss.contains("-readonly"));
        assertTrue("Нет invalid", scss.contains("-error"));
    }

    @Test
    public void referenceV5Palette() throws IOException {
        String scss = read(SCSS);
        for (String token : Arrays.asList("$md5-app-bg", "$md5-surface", "$md5-surface-2",
                "$md5-header", "$md5-text", "$md5-text-2", "$md5-border", "$md5-divider",
                "$md5-hover", "$md5-selected", "$md5-accent", "$md5-focus", "$md5-invalid")) {
            assertTrue("Отсутствует токен " + token, scss.contains(token));
        }
        // Референсные значения (PNG 1600x1000)
        for (String color : Arrays.asList("#151a20", "#1c232c", "#232c37", "#2a3440",
                "#26303b", "#e8edf3", "#aeb7c2", "#33404d", "#2f3944", "#222c37",
                "#ffb11b", "#7ea7d8", "#e26b63")) {
            assertTrue("Нет референсного цвета " + color + " в SCSS", scss.contains(color));
        }
        // Янтарная акцентная линия selected (6px слева)
        assertTrue("Нет янтарной линии selected (inset 6px #ffb11b)",
                scss.contains("inset 6px 0 0 $md5-accent"));
    }

    @Test
    public void darkDefaults() throws IOException {
        String defaults = read(DEFAULTS);
        assertTrue("Фон приложения не тёмный",
                defaults.contains("$v-app-background-color: #151a20;"));
        assertTrue("Основной текст не светлый",
                defaults.contains("$v-font-color: #e8edf3;"));
    }

    @Test
    public void themeIsolation() throws IOException {
        for (String theme : OTHER_THEMES) {
            String styles = readOrEmpty(theme + "/styles.scss");
            assertTrue("Тема " + theme + " импортирует вариант 5",
                    !styles.contains("modern-dark-component-style-v5"));
        }
        String scss = read(SCSS);
        assertTrue("Собственный корневой селектор (двойной префикс)",
                !scss.matches("(?m)^\\.hunttech-modern-dark \\{"));
    }

    @Test
    public void noGlobalSelectorsOutsideTheme() throws IOException {
        String scss = read(SCSS);
        for (String sel : Arrays.asList("\n.v-grid {", "\n.v-table {", "\n.v-tree {",
                "\n.v-textarea {", "\n.v-richtextarea {")) {
            assertTrue("Запрещённый глобальный селектор: " + sel, !scss.contains(sel));
        }
    }

    private String readOrEmpty(String relativePath) throws IOException {
        Path p = projectRoot().resolve(relativePath);
        if (!Files.exists(p)) {
            return "";
        }
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    private String read(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8
        );
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
