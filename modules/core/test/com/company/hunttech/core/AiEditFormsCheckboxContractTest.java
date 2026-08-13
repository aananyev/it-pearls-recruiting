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
 * Контракт чекбоксов AI Edit-форм HRM HuntTech.
 *
 * Чекбоксы форм блока «Управление AI» используют ОБЩИЕ стили темы CUBA Platform
 * (Valo): в локальных партиалах форм нет кастомных правил квадратика/подписи —
 * штатное выравнивание и отступы темы исключают наезд чекбокса на элементы под ним
 * и смещение квадратика относительно подписи.
 *
 * VacancyPromptTemplate намеренно не содержит чекбоксов — контракт фиксирует и это.
 */
public class AiEditFormsCheckboxContractTest {

    private static final String[] THEMES = {
            "halo",
            "havana",
            "helium",
            "hover",
            "hunttech-modern",
            "hunttech-modern-dark",
            "hunttech-modern-light"
    };

    /** Формы блока «Управление AI», в XML которых есть <checkBox>: slug папки → kebab имени partial. */
    private static final Map<String, String> FORMS_WITH_CHECKBOXES = new LinkedHashMap<>();

    static {
        FORMS_WITH_CHECKBOXES.put("aifunctionconfiguration", "ai-function-configuration");
        FORMS_WITH_CHECKBOXES.put("adminaiconfiguration", "admin-ai-configuration");
        FORMS_WITH_CHECKBOXES.put("useraifunctionoverride", "user-ai-function-override");
        FORMS_WITH_CHECKBOXES.put("useraiconfiguration", "user-ai-configuration");
    }

    /** Форма блока без чекбоксов — контрактно не требует стилизации. */
    private static final String FORM_WITHOUT_CHECKBOXES = "vacancyprompttemplate";

    @Test
    public void aiEditFormsCheckboxesUseCommonThemeStyles() throws IOException {
        for (Map.Entry<String, String> entry : FORMS_WITH_CHECKBOXES.entrySet()) {
            String slug = entry.getKey();
            String form = entry.getValue();
            String canon = readProjectFile(
                    "modules/web/themes/hover/com.company.hunttech/" + form + "-editor.scss");

            // Чекбоксы НЕ кастомизированы в партиалах форм: общие стили темы CUBA (Valo).
            assertFalse(form + ": в партиале осталось локальное правило .v-checkbox",
                    canon.contains(".edit-card .v-checkbox"));
            // Признак кастомного квадратика чекбокса (padding-left 28px — 20px квадратик + зазор).
            assertFalse(form + ": остался кастомный отступ подписи чекбокса",
                    canon.contains("padding-left: 28px"));

            // Форма использует общий Edit-контракт (карточки .edit-card), из которого
            // чекбоксы получают штатные стили темы.
            assertTrue(form + ": нет карточек .edit-card контракта", canon.contains(".edit-card"));

            for (String theme : THEMES) {
                String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
                assertTrue(theme + ": styles.scss не импортирует " + form + "-editor",
                        styles.contains(form + "-editor"));
                assertTrue(theme + ": styles.scss не вызывает @include " + form + "-editor-theme",
                        styles.contains("@include " + form + "-editor-theme;"));

                String local = readProjectFile(
                        "modules/web/themes/" + theme + "/com.company.hunttech/" + form + "-editor.scss");
                assertTrue(form + "-editor.scss не идентичен в теме " + theme, canon.equals(local));
            }
        }
    }

    @Test
    public void vacancyPromptTemplateHasNoCheckboxes() throws IOException {
        String xml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/" + FORM_WITHOUT_CHECKBOXES
                        + "/vacancy-prompt-template-edit.xml");
        assertFalse(FORM_WITHOUT_CHECKBOXES + ": XML не должен содержать чекбоксы",
                xml.contains("<checkBox"));
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
