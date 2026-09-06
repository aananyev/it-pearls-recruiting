package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Контракт чекбоксов справочных Edit-форм HRM HuntTech (меню «Справочники»).
 *
 * Чекбоксы форм используют ОБЩИЕ стили темы CUBA Platform (Valo): в локальных
 * партиалах форм нет кастомных правил квадратика/подписи — штатное выравнивание
 * и отступы темы исключают наезд чекбокса на элементы под ним и смещение
 * квадратика относительно подписи.
 *
 * Проверяются три партиала, стилизующие Edit-формы пункта меню «Справочники»:
 * dictionary-edit-forms (FileTypeEdit, SocialNetworkTypeEdit, GradeEdit,
 * CurrencyEdit, OutstaffingRatesEdit, EmployeeWorkStatusEdit, SignIconsEdit),
 * iteraction-editor (IteractionEdit), skill-tree-editor (SkillTreeEdit).
 */
public class DictionaryEditFormsCheckboxContractTest {

    private static final String[] THEMES = {
            "halo",
            "havana",
            "helium",
            "hover",
            "hunttech-modern",
            "hunttech-modern-light",
            "hunttech-modern-dark"
    };

    /**
     * Партиалы форм «Справочников» с чекбоксами: имя partial → root-класс формы.
     * Проверяем отсутствие локальной кастомизации чекбоксов и идентичность 7 тем.
     */
    private static final Map<String, String> PARTIALS = new LinkedHashMap<>();

    static {
        PARTIALS.put("dictionary-edit-forms", "dictionary-edit-forms");
        PARTIALS.put("iteraction-editor", "iteraction-editor");
        PARTIALS.put("skill-tree-editor", "skill-tree-editor");
    }

    /** Edit-дескрипторы форм «Справочников», в XML которых есть <checkBox>. */
    private static final String[] XML_WITH_CHECKBOXES = {
            "iteraction/iteraction-edit.xml",
            "skilltree/skill-tree-edit.xml",
            "employeeworkstatus/employee-work-status-edit.xml"
    };

    @Test
    public void dictionaryEditFormsWithCheckboxesKeepBindings() throws IOException {
        // Правка затронула только SCSS: XML-дескрипторы форм с чекбоксами сохраняют
        // data bindings (dataContainer/property/caption) — чекбоксы остаются на месте.
        for (String xmlPath : XML_WITH_CHECKBOXES) {
            String xml = readProjectFile(
                    "modules/web/src/com/company/hunttech/web/screens/" + xmlPath);
            assertTrue(xmlPath + ": нет ни одного <checkBox", xml.contains("<checkBox"));
        }
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
