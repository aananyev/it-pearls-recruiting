package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Visual contract тест редизайна табличных и текстовых компонентов HRM HuntTech:
 * DataGrid (.v-grid), Table (.v-table), TreeTable (CUBA Tree: .v-tree),
 * TextArea (.v-textarea), RichTextArea (.v-richtextarea).
 *
 * Защищает: подключение общих токенов и mixin во всех фактических темах,
 * различия утверждённых вариантов (Halo=1, hunttech-modern-light=3,
 * hunttech-modern-dark=5), наличие состояний и отсутствие изменений
 * нецелевых компонентов.
 */
public class ThemeTableTextComponentsContractTest {

    private static final List<String> THEMES = Arrays.asList(
            "halo",
            "havana",
            "helium",
            "hover",
            "hunttech-modern",
            "hunttech-modern-light",
            "hunttech-modern-dark"
    );

    private static final String COMMON_SCSS = "table-text-components.scss";
    private static final String TOKENS_SUFFIX = "-table-text-tokens.scss";

    private static final List<String> REQUIRED_TOKENS = Arrays.asList(
            "$hrm-table-bg:", "$hrm-table-text:", "$hrm-table-border:",
            "$hrm-table-header-bg:", "$hrm-table-header-text:", "$hrm-table-divider:",
            "$hrm-table-hover:", "$hrm-table-selected:", "$hrm-table-selected-border:",
            "$hrm-table-focus:", "$hrm-table-zebra:", "$hrm-table-sort:", "$hrm-table-resize:",
            "$hrm-editor-bg:", "$hrm-editor-text:", "$hrm-editor-border:",
            "$hrm-editor-focus:", "$hrm-editor-invalid:",
            "$hrm-editor-disabled-bg:", "$hrm-editor-readonly-bg:", "$hrm-editor-placeholder:",
            "$hrm-scrollbar-track:", "$hrm-scrollbar-thumb:",
            "$hrm-rich-toolbar-bg:", "$hrm-rich-toolbar-border:"
    );

    @Test
    public void commonScssIsIdenticalInAllThemes() throws IOException {
        String reference = null;
        for (String theme : THEMES) {
            String scss = readProjectFile(themeScssPath(theme, COMMON_SCSS));
            if (reference == null) {
                reference = scss;
            } else {
                assertEquals("Общий SCSS компонентов различается между темами", reference, scss);
            }
        }
        assertNotNull(reference);
        assertTrue(reference.contains("@mixin hrm-table-text-components"));
    }

    @Test
    public void stylesScssConnectsTokensAndMixinInAllThemes() throws IOException {
        for (String theme : THEMES) {
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            String tokens = "@import \"com.company.hunttech/" + theme + "-table-text-tokens\";";
            String common = "@import \"com.company.hunttech/table-text-components\";";
            String include = "@include hrm-table-text-components;";

            int sharedImport = styles.indexOf("@import \"com.company.hunttech/edit-screen-shared-styles\";");
            int tokensImport = styles.indexOf(tokens);
            int commonImport = styles.indexOf(common);
            int sharedInclude = styles.indexOf("@include edit-screen-shared-styles;");
            int commonInclude = styles.indexOf(include);

            assertTrue("Токены не подключены в теме " + theme, tokensImport >= 0);
            assertTrue("Общий mixin не подключён в теме " + theme, commonImport >= 0);
            assertTrue("Токены должны импортироваться после shared-слоя", tokensImport > sharedImport);
            assertTrue("Общий mixin должен импортироваться после shared-слоя", commonImport > sharedImport);
            assertTrue("include mixin отсутствует в теме " + theme, commonInclude > sharedInclude);
        }
    }

    @Test
    public void tokensDefineFullPaletteInAllThemes() throws IOException {
        for (String theme : THEMES) {
            String tokens = readProjectFile(themeScssPath(theme, theme + TOKENS_SUFFIX));
            for (String required : REQUIRED_TOKENS) {
                assertTrue("В теме " + theme + " отсутствует токен " + required,
                        tokens.contains(required));
            }
        }
    }

    @Test
    public void approvedThemeVariantsDiffer() throws IOException {
        // Halo — вариант 1: светлый голубой (белая поверхность)
        String halo = readProjectFile(themeScssPath("halo", "halo" + TOKENS_SUFFIX));
        assertTrue("Halo: ожидается светлая поверхность", halo.contains("$hrm-table-bg: #ffffff;"));

        // hunttech-modern-light — вариант 3: тёплый светло-серый (молочная поверхность)
        String light = readProjectFile(themeScssPath("hunttech-modern-light", "hunttech-modern-light" + TOKENS_SUFFIX));
        assertTrue("Light: ожидается тёплая молочная поверхность", light.contains("$hrm-table-bg: #fbfaf8;"));

        // hunttech-modern-dark — вариант 5: тёмная нейтральная поверхность
        String dark = readProjectFile(themeScssPath("hunttech-modern-dark", "hunttech-modern-dark" + TOKENS_SUFFIX));
        assertTrue("Dark: ожидается тёмная поверхность", dark.contains("$hrm-table-bg: #232a33;"));
        assertFalse("Dark: светлый фон таблиц запрещён (вариант 5 — тёмный)",
                dark.contains("$hrm-table-bg: #ffffff;"));
        assertTrue("Dark: светлый читаемый текст", dark.contains("$hrm-table-text: #dde4ec;"));
        assertTrue("Dark: акцентная граница selected", dark.contains("$hrm-table-selected-border: #5c9dff;"));

        // Палитры Halo / Light / Dark попарно различны
        assertFalse("Halo и Light не должны иметь одинаковую палитру",
                halo.equals(light) || halo.contains("$hrm-table-bg: #fbfaf8;"));
        assertFalse("Light и Dark не должны иметь одинаковую палитру",
                light.equals(dark) || dark.contains("$hrm-table-bg: #fbfaf8;"));
    }

    @Test
    public void mixinCoversAllFiveComponentsAndStates() throws IOException {
        String scss = readProjectFile(themeScssPath("hover", COMMON_SCSS));

        // Пять целевых компонентов
        assertTrue("DataGrid не покрыт", scss.contains(".v-grid"));
        assertTrue("Table не покрыта", scss.contains(".v-table"));
        assertTrue("TreeTable (CUBA Tree) не покрыт", scss.contains(".v-tree"));
        assertTrue("TextArea не покрыт", scss.contains(".v-textarea"));
        assertTrue("RichTextArea не покрыт", scss.contains(".v-richtextarea"));

        // Состояния
        assertTrue("hover отсутствует", scss.contains(":hover"));
        assertTrue("selected отсутствует", scss.contains("-selected"));
        assertTrue("focus отсутствует", scss.contains("focused"));
        assertTrue("invalid отсутствует", scss.contains("-error"));
        assertTrue("disabled отсутствует", scss.contains(".v-disabled"));
        assertTrue("read-only отсутствует", scss.contains(".v-readonly"));

        // Детали компонентов
        assertTrue("expander/иерархия TreeTable отсутствует", scss.contains(".v-tree-node-children"));
        assertTrue("toolbar RichTextArea отсутствует", scss.contains(".v-richtextarea-toolbar"));
        assertTrue("resize-ручка колонок отсутствует", scss.contains(".v-grid-column-resize-handle"));
        assertTrue("scrollbar токены не используются", scss.contains("$hrm-scrollbar-thumb"));
    }

    @Test
    public void mixinDoesNotTouchUnrelatedComponents() throws IOException {
        String scss = readProjectFile(themeScssPath("hover", COMMON_SCSS));

        List<String> forbidden = Arrays.asList(
                ".v-button",           // кнопки вне toolbar RichTextArea
                ".v-tabsheet",         // вкладки
                ".v-datefield",        // поля дат
                ".v-combobox",         // комбобоксы
                ".v-lookupfield",      // lookup-поля
                ".v-filterselect",     // фильтр-селекты
                ".v-menubar",          // меню
                ".v-window",           // окна
                ".v-layout",           // layout-контейнеры
                ".v-label"             // подписи
        );
        for (String selector : forbidden) {
            assertFalse("Нецелевой селектор " + selector + " попал в общий слой компонентов",
                    scss.contains(selector));
        }
    }

    private String themeScssPath(String theme, String file) {
        return "modules/web/themes/" + theme + "/com.company.hunttech/" + file;
    }

    private String readProjectFile(String relativePath) throws IOException {
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
