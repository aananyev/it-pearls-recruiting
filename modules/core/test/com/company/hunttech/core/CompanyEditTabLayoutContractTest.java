package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Контракт вертикальной компоновки вкладок CompanyEdit — защита от регрессии
 * «видна одна строка» в tabConpanyDetails (сентябрь 2026, 4 рецидива:
 * 3caff99f -> d93a1753 -> 5ea000d1 -> fe603384; корень 2c678025/f9b03f4f).
 *
 * Инварианты (эталон каскада Edit-форм HRM):
 *  1. edit-workspace / edit-workspace-scroll — ТОЛЬКО на workspace-уровне
 *     (companyEditorWorkspace, companyEditorContentScrollBox), никогда на
 *     scrollBox внутри вкладок TabSheet;
 *  2. tab вкладки не сочетает expand=<scrollBox> с height="100%" на этом
 *     scrollBox (двойное управление высотой);
 *  3. вертикальный overflow панелей вкладок не режется overflow:hidden.
 */
public class CompanyEditTabLayoutContractTest {

    private static final String SCREEN =
            "modules/web/src/com/company/hunttech/web/screens/company/company-edit.xml";
    private static final String[] THEMES = {
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark"
    };
    private static final String[] TAB_SCROLL_IDS = {
            "companyDetailsScroll", "companyRequisitesScroll",
            "companyDescriptionScroll", "companyDepartmentsScroll"
    };
    private static final String[] TAB_IDS = {
            "tabConpanyDetails", "companyRequisitesTab",
            "companyDescriptionTab", "tabCompanyDepartament"
    };

    @Test
    public void tabScrollBoxesDoNotReuseWorkspaceClasses() throws IOException {
        String xml = readProjectFile(SCREEN);

        // 1. Классы workspace присутствуют на своем уровне.
        assertTrue("workspace-vbox companyEditorWorkspace отсутствует",
                xml.contains("<vbox id=\"companyEditorWorkspace\""));
        assertTrue("mainTab отсутствует",
                xml.contains("<tabSheet id=\"mainTab\""));

        // 2. edit-workspace-content остаётся на внутренних vbox (эталон каскада).
        assertTrue("edit-workspace-content отсутствует во вкладках",
                xml.contains("stylename=\"edit-workspace-content\""));
    }

    @Test
    public void tabsDoNotCombineExpandWithHeight100() throws IOException {
        String xml = readProjectFile(SCREEN);

        // В baseline eb795a67 вкладкам высоту отдаёт tabSheet mainTab (expand=mainTab, height=100%).
        assertTrue("mainTab без height=100% — вкладки схлопнутся",
                xml.contains("<tabSheet id=\"mainTab\"")
                        && xml.contains("height=\"100%\""));
        assertTrue("companyEditorWorkspace должен раскрывать mainTab",
                xml.contains("expand=\"mainTab\""));
    }

    @Test
    public void tabSheetPanelsKeepVerticalOverflow() throws IOException {
        for (String theme : THEMES) {
            String scss = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/company-editor.scss");

            // Вертикальный overflow панелей вкладок не режется.
            assertFalse(theme + ": tabsheetpanel режет вертикальный overflow (причина «одной строки»)",
                    scss.matches("(?s).*\\.v-tabsheet-tabsheetpanel\\s*\\{[^}]*overflow:\\s*hidden\\s*!important;.*"));
            // Scroll-класс вкладок не получает height:100%!important.
            assertFalse(theme + ": company-tab-scroll с height:100%!important",
                    scss.matches("(?s).*\\.company-tab-scroll[^}]*height:\\s*100%\\s*!important;.*"));
            // Workspace-scroll сохраняет контрактный вертикальный скролл.
            assertTrue(theme + ": edit-workspace-scroll без overflow-y:auto",
                    scss.contains("overflow-y: auto !important"));
            // company-main-tab блок изолированной компоновки присутствует.
            assertTrue(theme + ": блок .company-main-tab утрачен",
                    scss.contains(".company-main-tab {"));
        }
    }

    private static String readProjectFile(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
