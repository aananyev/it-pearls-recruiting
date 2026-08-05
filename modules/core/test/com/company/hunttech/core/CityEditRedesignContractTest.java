package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает информационную архитектуру CityEdit: два логических раздела,
 * соответствующую label-навигацию и неизменённые CUBA data/action-контракты.
 */
public class CityEditRedesignContractTest {

    @Test
    public void cityEditUsesTwoSectionNavigationAndPreservesBindings() throws IOException {
        String xml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/city/city-edit.xml");
        String java = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/city/CityEdit.java");

        assertTrue(xml.contains("id=\"cityIdentityNav\" caption=\"Наименование\""));
        assertTrue(xml.contains("invoke=\"focusMainSection\""));
        assertTrue(xml.contains("id=\"cityRegionNav\" caption=\"Регион и связь\""));
        assertTrue(xml.contains("invoke=\"focusRegionSection\""));
        assertTrue(xml.contains("id=\"cityIdentitySection\" caption=\"Наименование\""));
        assertTrue(xml.contains("id=\"cityRegionSection\" caption=\"Регион и связь\""));

        // Редизайн не должен менять bindings, loader, JPQL и framework actions.
        assertTrue(xml.contains("id=\"cityDc\" class=\"com.company.hunttech.entity.City\" view=\"city-edit-view\""));
        assertTrue(xml.contains("id=\"cityRegionsLc\" cacheable=\"true\""));
        assertTrue(xml.contains("select e from hunttech_Region e"));
        assertTrue(xml.contains("property=\"cityRuName\""));
        assertTrue(xml.contains("property=\"cityPhoneCode\""));
        assertTrue(xml.contains("optionsContainer=\"cityRegionsDc\" property=\"cityRegion\""));
        assertTrue(xml.contains("action id=\"lookup\" type=\"picker_lookup\""));
        assertTrue(xml.contains("action=\"windowCommitAndClose\""));
        assertTrue(xml.contains("action=\"windowClose\""));

        assertTrue(java.contains("focusMainSection()"));
        assertTrue(java.contains("focusRegionSection()"));
        assertTrue(java.contains("addStyleName(ACTIVE_NAV_STYLE)"));
        assertTrue(java.contains("removeStyleName(ACTIVE_NAV_STYLE)"));
    }

    private String readProjectFile(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8);
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
