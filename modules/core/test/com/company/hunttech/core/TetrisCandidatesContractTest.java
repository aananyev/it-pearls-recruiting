package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Контрактный тест модуля «Тетрис с кандидатами» (TetrisCandidates).
 * Проверяет валидность XML-дескриптора, контроллера, модели дашборда и идентичность SCSS во всех 7 темах.
 */
public class TetrisCandidatesContractTest {

    private static final String[] THEMES = {
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark"
    };

    @Test
    public void tetrisCandidatesDescriptorIsValidAndModernized() throws IOException {
        String xml = source("modules/web/src/com/company/hunttech/web/screens/fragments/tetriscandidates/tetris-candidates.xml");

        // Проверка отсутствия синтаксических ошибок (например, лишней скобки '>>')
        assertFalse("Дескриптор не должен содержать синтаксическую опечатку >>", xml.contains("</dashboard:dashboard>>"));
        assertTrue(xml.contains("</dashboard:dashboard>"));

        // Проверка наличия стилизованной рабочей области и шапки-тулбара
        assertTrue(xml.contains("stylename=\"tetris-candidates-workspace\""));
        assertTrue(xml.contains("id=\"tetrisHeaderToolbar\""));
        assertTrue(xml.contains("id=\"tetrisTitleIcon\""));
        assertTrue(xml.contains("id=\"tetrisHeaderTitle\""));
        assertTrue(xml.contains("id=\"tetrisCandidatesDashboard\""));
        assertTrue(xml.contains("code=\"tetris-candidates-dashboard\""));
    }

    @Test
    public void tetrisCandidatesControllerSupportsDirectJsonPath() throws IOException {
        String java = source("modules/web/src/com/company/hunttech/web/screens/fragments/tetriscandidates/TetrisCandidates.java");

        assertTrue(java.contains("UiController(\"hunttech_TetrisCandidates\")"));
        assertTrue(java.contains("DASHBOARD_CODE = \"tetris-candidates-dashboard\""));
        assertTrue(java.contains("DASHBOARD_JSON = \"com/company/hunttech/web/screens/mainscreen/dashboards/tetris-candidates-dashboard.json\""));
        assertTrue(java.contains("tetrisCandidatesDashboard.setJsonPath(DASHBOARD_JSON)"));
    }

    @Test
    public void tetrisCandidatesDashboardJsonExistsAndValid() throws IOException {
        String json = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/dashboards/tetris-candidates-dashboard.json");

        assertTrue(json.contains("\"title\": \"Тетрис с кандидатами\""));
        assertTrue(json.contains("\"code\": \"tetris-candidates-dashboard\""));
        assertTrue(json.contains("\"frameId\": \"hunttech_MyActiveCandidatesDashboard\""));
        assertTrue(json.contains("\"className\": \"com.haulmont.addon.dashboard.model.visualmodel.RootLayout\""));
        assertTrue(json.contains("\"className\": \"com.haulmont.addon.dashboard.model.visualmodel.WidgetLayout\""));
    }

    @Test
    public void allSevenThemesIncludeIdenticalTetrisCandidatesScss() throws IOException {
        String canonical = null;

        for (String theme : THEMES) {
            String scssPath = "modules/web/themes/" + theme + "/com.company.hunttech/tetris-candidates.scss";
            String scss = source(scssPath);
            String styles = source("modules/web/themes/" + theme + "/styles.scss");

            assertTrue(theme + ": styles.scss должен импортировать tetris-candidates",
                    styles.contains("tetris-candidates"));
            assertTrue(theme + ": styles.scss должен вызывать @include tetris-candidates-theme",
                    styles.contains("@include tetris-candidates-theme;"));

            assertTrue(scss.contains("@mixin tetris-candidates-theme"));
            assertTrue(scss.contains(".tetris-candidates-workspace"));
            assertTrue(scss.contains(".tetris-header-toolbar"));
            assertTrue(scss.contains(".text-block-gradient-green"));
            assertTrue(scss.contains(".text-block-gradient-yellow"));
            assertTrue(scss.contains(".text-block-gradient-red"));

            if (canonical == null) {
                canonical = scss;
            } else {
                assertEquals("Файл tetris-candidates.scss должен быть идентичен во всех темах (" + theme + ")",
                        canonical, scss);
            }
        }
    }

    @Test
    public void specificationDocumentExists() throws IOException {
        String doc = source("docs/ui/TetrisCandidates_Spec.md");

        assertTrue(doc.contains("Тетрис с кандидатами"));
        assertTrue(doc.contains("hunttech_TetrisCandidates"));
        assertTrue(doc.contains("hunttech_MyActiveCandidatesDashboard"));
        assertTrue(doc.contains("text-block-gradient-green"));
    }

    private static String source(String relativePath) throws IOException {
        Path path = Paths.get(relativePath);
        if (!Files.exists(path)) {
            path = Paths.get("../..", relativePath);
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
