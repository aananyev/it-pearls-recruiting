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
 * Visual contract тест варианта 3 (тёплый светло-серый) темы hunttech-modern-light:
 * DataGrid, Table, TreeTable, TextArea, RichTextArea.
 *
 * Проверяет фактическую цепочку импорта (styles.scss → component partial),
 * наличие компонентов и состояний, изоляцию от других тем и отсутствие
 * глобальных селекторов вне корневого селектора темы.
 */
public class ModernLightV3ContractTest {

    private static final String THEME = "modules/web/themes/hunttech-modern-light";
    private static final String SCSS = THEME + "/com.company.hunttech/modern-light-component-style-v3.scss";
    private static final String STYLES = THEME + "/styles.scss";
    private static final String DEFAULTS = THEME + "/hunttech-modern-light-defaults.scss";

    private static final List<String> OTHER_THEMES = Arrays.asList(
            "modules/web/themes/halo",
            "modules/web/themes/havana",
            "modules/web/themes/helium",
            "modules/web/themes/hover",
            "modules/web/themes/hunttech-modern",
            "modules/web/themes/hunttech-modern-dark"
    );

    @Test
    public void scssConnectedInsideRootSelector() throws IOException {
        String styles = read(STYLES);
        // Импорт присутствует и расположен ВНУТРИ корневого селектора темы
        assertTrue("modern-light-component-style-v3 не подключён в styles.scss",
                styles.contains("@import \"com.company.hunttech/modern-light-component-style-v3\";"));
        int importIdx = styles.indexOf("@import \"com.company.hunttech/modern-light-component-style-v3\";");
        int openIdx = styles.indexOf(".hunttech-modern-light {");
        int closeIdx = styles.lastIndexOf("}");
        assertTrue("Импорт не внутри корневого селектора .hunttech-modern-light",
                importIdx > openIdx && importIdx < closeIdx);
    }

    @Test
    public void allFiveComponentsPresent() throws IOException {
        String scss = read(SCSS);
        List<String> components = Arrays.asList(
                ".v-grid-header",        // DataGrid
                ".v-table-header-wrap",  // Table
                ".v-treegrid-header",    // TreeTable (TreeDataGrid)
                ".v-textarea",           // TextArea
                ".v-richtextarea"        // RichTextArea
        );
        for (String sel : components) {
            assertTrue("Отсутствует компонент " + sel, scss.contains(sel));
        }
        // CUBA TreeTable (<treeTable>) тоже покрыт (Vaadin Tree)
        assertTrue("Отсутствует .v-tree (TreeTable)", scss.contains(".v-tree"));
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
    public void warmVariant3Palette() throws IOException {
        String scss = read(SCSS);
        // Токены варианта 3
        for (String token : Arrays.asList("$ml3-app-bg", "$ml3-surface", "$ml3-header-bg",
                "$ml3-text", "$ml3-border", "$ml3-hover", "$ml3-selected", "$ml3-accent")) {
            assertTrue("Отсутствует токен " + token, scss.contains(token));
        }
        // Тёплые значения палитры
        assertTrue("Фон не тёплый (#f3f1ec)", scss.contains("#f3f1ec"));
        assertTrue("Header не тёплый (#ece9e2)", scss.contains("#ece9e2"));
        assertTrue("Selected не серо-бежевый (#e3dccb)", scss.contains("#e3dccb"));
        assertTrue("Граница не серо-бежевая (#d9d4ca)", scss.contains("#d9d4ca"));
        // Фирменный акцент
        assertTrue("Нет акцента #c1211f", scss.contains("#c1211f"));
    }

    @Test
    public void themeIsolation() throws IOException {
        // Другие темы не импортируют файл варианта 3 и не содержат его токенов
        for (String theme : OTHER_THEMES) {
            String styles = readOrEmpty(theme + "/styles.scss");
            assertTrue("Тема " + theme + " импортирует вариант 3",
                    !styles.contains("modern-light-component-style-v3"));
        }
        String scss = read(SCSS);
        assertTrue("Нет тёплого фона приложения в component-файле (должен быть в defaults)",
                !scss.contains("$v-app-background-color"));
    }

    @Test
    public void noGlobalSelectorsOutsideTheme() throws IOException {
        String scss = read(SCSS);
        // Запрещены топ-левел селекторы без префикса темы
        List<String> forbidden = Arrays.asList(
                "\n.v-grid {", "\n.v-table {", "\n.v-tree {", "\n.v-textarea {", "\n.v-richtextarea {",
                "\n.v-grid:", "\n.v-table:", "\n.v-textarea:", "\n.v-richtextarea:"
        );
        for (String sel : forbidden) {
            assertTrue("Запрещённый глобальный селектор: " + sel, !scss.contains(sel));
        }
    }

    @Test
    public void warmAppBackgroundInDefaults() throws IOException {
        String defaults = read(DEFAULTS);
        assertTrue("Фон рабочей области не тёплый",
                defaults.contains("$v-app-background-color: #f3f1ec;"));
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
