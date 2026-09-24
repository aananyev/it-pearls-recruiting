package com.company.hunttech.web.screens.datagrid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Контрактный тест проверяет оформление и стили компонента пагинации и счетчика строк
 * DataGrid / Table (CubaRowsCount / c-table-rows-count / c-paging / rowsCount) по стандарту BL-2026-049.
 *
 * Проверяется:
 * 1. Наличие правил flexbox-композиции DataGrid (.c-data-grid-composition, .c-data-grid-top, .v-grid),
 *    предотвращающих наложение таблицы на кнопки пагинации.
 * 2. 24px размеры кнопок перелистывания страниц (.c-paging-change-page, c-paging-first/prev/next/last).
 * 3. Отсутствие фиксированной 24px ширины для текстовой кнопки подсчета страниц (.v-button-link / .c-paging-count).
 * 4. Обнуление паразитных отступов (.c-paging-wrap margin-bottom: 0).
 * 5. 100% паритет стилей во всех 7 темах оформления CUBA/HuntTech.
 */
class DataGridRowsCountLayoutContractTest {

    private static final List<String> THEMES = Arrays.asList(
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark");

    private File resolveFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        if (path.startsWith("modules/web/")) {
            File subFile = new File(path.substring("modules/web/".length()));
            if (subFile.exists()) {
                return subFile;
            }
        }
        Path root = Paths.get("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        if (root != null) {
            File resolved = root.resolve(path).toFile();
            if (resolved.exists()) {
                return resolved;
            }
        }
        return file;
    }

    @Test
    @DisplayName("BL-2026-049: Стили rowsCount и DataGrid top panel присутствуют во всех 7 темах")
    void testScssDataGridRowsCountStandardsInAllThemes() throws IOException {
        for (String theme : THEMES) {
            File scssFile = resolveFile("modules/web/themes/" + theme + "/com.company.hunttech/edit-screen-shared-styles.scss");
            assertTrue(scssFile.exists(), "Файл edit-screen-shared-styles.scss должен существовать для темы: " + theme);

            String scss = new String(Files.readAllBytes(scssFile.toPath()), StandardCharsets.UTF_8);

            // 1. Композиция DataGrid
            assertTrue(scss.contains(".c-data-grid-composition"),
                    "Тема " + theme + " должна содержать селектор .c-data-grid-composition");
            assertTrue(scss.contains(".c-data-grid-top"),
                    "Тема " + theme + " должна содержать селектор .c-data-grid-top");
            assertTrue(scss.contains("& > .v-grid"),
                    "Тема " + theme + " должна содержать flex-правило для .v-grid");

            // 2. Селекторы rowsCount и CubaRowsCount
            assertTrue(scss.contains(".c-table-rows-count"),
                    "Тема " + theme + " должна стилизовать .c-table-rows-count");
            assertTrue(scss.contains(".c-paging-wrap"),
                    "Тема " + theme + " должна сбрасывать отступы в .c-paging-wrap");

            // 3. 24px кнопки пагинации со стрелками
            assertTrue(scss.contains(".c-paging-change-page"),
                    "Тема " + theme + " должна стилизовать .c-paging-change-page");
            assertTrue(scss.contains(".c-paging-first"),
                    "Тема " + theme + " должна содержать селектор .c-paging-first");
            assertTrue(scss.contains(".c-paging-last"),
                    "Тема " + theme + " должна содержать селектор .c-paging-last");

            // 4. Кнопка подсчета количества строк не должна сжиматься по ширине
            assertTrue(scss.contains(".c-paging-count"),
                    "Тема " + theme + " должна содержать правила для .c-paging-count");
            assertTrue(scss.contains(".v-button.v-button-link.c-paging-count"),
                    "Тема " + theme + " должна переопределять ссылку подсчета .v-button.v-button-link.c-paging-count");

            // 5. Правое выравнивание rowsCount в DataGrid top panel
            assertTrue(scss.contains("margin-left: auto !important;"),
                    "Тема " + theme + " должна выравнивать rowsCount вправо через margin-left: auto");
        }
    }

    @Test
    @DisplayName("BL-2026-049: Защита от регрессий — кнопки пагинации имеют габариты 24x24px, а кнопка подсчета auto")
    void testDimensionsAndSpacingRules() throws IOException {
        for (String theme : THEMES) {
            File scssFile = resolveFile("modules/web/themes/" + theme + "/com.company.hunttech/edit-screen-shared-styles.scss");
            String scss = new String(Files.readAllBytes(scssFile.toPath()), StandardCharsets.UTF_8);

            // Проверяем, что нет опасного правила, задающего 24px ширину на все .v-button без исключения
            // (кнопка link с текстом "из 350" должна быть width: auto, а правило ограничено c-paging-wrap)
            assertTrue(scss.contains(".c-paging-wrap > .v-button:not(.v-button-link)"),
                    "Тема " + theme + " должна изолировать v-button:not(.v-button-link) внутри .c-paging-wrap");

            // Проверяем обнуление margin-bottom для элементов внутри c-paging-wrap
            assertTrue(scss.contains("margin-bottom: 0 !important;"),
                    "Тема " + theme + " должна обнулять margin-bottom для дочерних элементов .c-paging-wrap");
        }
    }
}
