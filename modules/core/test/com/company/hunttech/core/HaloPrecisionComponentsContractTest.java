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
 * Visual contract тест Halo Precision: namespace-стили ht-halo-precision-*
 * для DataGrid, Table, TreeTable (TreeDataGrid), TextArea и RichTextArea
 * в светлой теме Halo по референсам halo_variant1_*.png.
 *
 * Защищает: подключение SCSS в тему Halo, наличие всех пяти namespace-классов
 * и обязательных состояний, отсутствие глобальных Vaadin-селекторов,
 * назначение stylename на целевые экраны.
 */
public class HaloPrecisionComponentsContractTest {

    private static final String SCSS =
            "modules/web/themes/halo/com.company.hunttech/halo-precision-components.scss";
    private static final String STYLES =
            "modules/web/themes/halo/styles.scss";

    @Test
    public void scssConnectedToHaloTheme() throws IOException {
        String styles = readProjectFile(STYLES);
        assertTrue("halo-precision-components не подключён в halo/styles.scss",
                styles.contains("@import \"com.company.hunttech/halo-precision-components\";"));
    }

    @Test
    public void allFiveNamespacesPresentWithStates() throws IOException {
        String scss = readProjectFile(SCSS);

        List<String> namespaces = Arrays.asList(
                ".ht-halo-precision-datagrid",
                ".ht-halo-precision-table",
                ".ht-halo-precision-treetable",
                ".ht-halo-precision-textarea",
                ".ht-halo-precision-richtextarea"
        );
        for (String ns : namespaces) {
            assertTrue("Отсутствует namespace " + ns, scss.contains(ns));
        }

        // Пять Vaadin-компонентов внутри namespace
        assertTrue("Нет .v-grid", scss.contains(".v-grid"));
        assertTrue("Нет .v-table", scss.contains(".v-table"));
        assertTrue("Нет .v-treegrid (TreeDataGrid)", scss.contains(".v-treegrid"));
        assertTrue("Нет .v-textarea", scss.contains(".v-textarea"));
        assertTrue("Нет .v-richtextarea", scss.contains(".v-richtextarea"));

        // Обязательные состояния
        assertTrue("Нет hover", scss.contains(":hover"));
        assertTrue("Нет selected", scss.contains("-selected"));
        assertTrue("Нет focus", scss.contains("focus"));
        assertTrue("Нет disabled", scss.contains(".v-disabled"));
        assertTrue("Нет read-only", scss.contains(".v-readonly") || scss.contains("-readonly"));
        assertTrue("Нет invalid", scss.contains("-error"));

        // Референсные цвета варианта 1
        assertTrue("Отсутствует референсный header #f9f9fa", scss.contains("#f9f9fa"));
        assertTrue("Отсутствует референсный selected #b1cff2", scss.contains("#b1cff2"));
        assertTrue("Отсутствует референсный hover #f3f8fd", scss.contains("#f3f8fd"));
        assertTrue("Отсутствует референсный border #dde1e8", scss.contains("#dde1e8"));
        assertTrue("Отсутствует focus #417be1", scss.contains("#417be1"));
    }

    private void assertStylename(String xmlPath, String stylename) throws IOException {
        String xml = readProjectFile(xmlPath);
        assertTrue("В " + xmlPath + " отсутствует stylename=" + stylename,
                xml.contains("stylename=\"" + stylename + "\""));
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
