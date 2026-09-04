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

        // 1. Классы workspace присутствуют на своих уровнях.
        assertTrue("workspace-vbox companyEditorWorkspace отсутствует",
                xml.contains("<vbox id=\"companyEditorWorkspace\""));
        assertTrue("общий scrollBox companyEditorContentScrollBox отсутствует",
                xml.contains("<scrollBox id=\"companyEditorContentScrollBox\""));

        // 2. ЗАПРЕТ: scrollBox вкладок несёт дубль edit-workspace/edit-workspace-scroll.
        for (String id : TAB_SCROLL_IDS) {
            int idx = xml.indexOf("id=\"" + id + "\"");
            assertTrue(id + ": scrollBox не найден", idx >= 0);
            int declEnd = xml.indexOf('>', idx);
            String decl = xml.substring(idx, declEnd);
            assertFalse(id + ": scrollBox вкладки несёт edit-workspace (дубль workspace-уровня)",
                    decl.contains("edit-workspace"));
            assertFalse(id + ": scrollBox вкладки несёт edit-workspace-scroll",
                    decl.contains("edit-workspace-scroll"));
            assertTrue(id + ": scrollBox вкладки без вкладочного класса company-tab-scroll",
                    decl.contains("company-tab-scroll"));
        }

        // 3. edit-workspace-content остаётся на внутренних vbox (эталон каскада).
        assertTrue("edit-workspace-content отсутствует во вкладках",
                xml.contains("stylename=\"edit-workspace-content\""));
    }

    @Test
    public void tabsDoNotCombineExpandWithHeight100() throws IOException {
        String xml = readProjectFile(SCREEN);

        for (String tabId : TAB_IDS) {
            int tabIdx = xml.indexOf("<tab id=\"" + tabId + "\"");
            assertTrue(tabId + ": вкладка не найдена", tabIdx >= 0);
            String tabDecl = xml.substring(tabIdx, xml.indexOf('>', tabIdx));
            // expand на tab в связке с height="100%" вложенного scrollBox —
            // двойное управление высотой (причина «видна одна строка»).
            assertFalse(tabId + ": tab сочетает expand с height-схемой scrollBox (двойная высота)",
                    tabDecl.contains("expand="));
        }

        // Высоту вкладкам отдаёт tabSheet: mainTab сохраняет height=100%.
        assertTrue("mainTab без height=100% — вкладки схлопнутся",
                xml.contains("<tabSheet id=\"mainTab\"")
                        && xml.contains("height=\"100%\""));
        int scrollIdx = xml.indexOf("id=\"companyEditorContentScrollBox\"");
        int tabIdx = xml.indexOf("id=\"mainTab\"");
        assertTrue("mainTab должен быть вложен в companyEditorContentScrollBox",
                scrollIdx >= 0 && tabIdx > scrollIdx);
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
