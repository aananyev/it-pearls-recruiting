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
 * Защищает presentation-контракт JobCandidateEdit: порядок sidebar по Edit-формам
 * HRM HuntTech и локальные SCSS-ограничения от переполнения без проверки бизнес-логики.
 */
public class JobCandidateEditLayoutContractTest {

    private static final String XML =
            "modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml";
    private static final String[] THEMES = {
            "halo",
            "havana",
            "helium",
            "hover",
            "hunttech-modern",
            "hunttech-modern-light",
            "hunttech-modern-dark"
    };

    @Test
    public void sidebarFollowsMandatoryEditFormOrder() throws IOException {
        String xml = readProjectFile(XML);

        assertOrdered(xml,
                "id=\"candidatePic\"",
                "id=\"fullNameField\"",
                "id=\"personPositionLabel\"",
                "id=\"candidateProfileSummary\"",
                "id=\"candidateNavigation\"",
                "id=\"candidateProfileContacts\"",
                "id=\"candidateSidebarSpacer\"",
                "id=\"candidateProfileFooter\"");

        assertTrue(xml.contains("stylename=\"job-candidate-navigation label-navigation\""));
        assertTrue(xml.contains("label-nav-item label-nav-item-active"));
    }

    @Test
    public void descriptorKeepsOverflowProtectionClasses() throws IOException {
        String xml = readProjectFile(XML);

        assertTrue(xml.contains("id=\"jobCandidateSidebar\""));
        assertTrue(xml.contains("width=\"312px\""));
        assertTrue(xml.contains("stylename=\"job-candidate-social-actions\""));
        assertTrue(xml.contains("id=\"tabResumeVbox\" spacing=\"true\" width=\"100%\" height=\"100%\""));
        assertTrue(xml.contains("id=\"tabCommentsVbox\" spacing=\"true\" width=\"100%\" height=\"100%\""));
        assertTrue(xml.contains("id=\"jobCandidateBottomBar\""));
        assertTrue(section(xml, "id=\"jobCandidateBottomBar\"", "</hbox>").contains("width=\"100%\""));
        assertTrue(xml.contains("id=\"cardAuditInfoButton\""));
        assertTrue(xml.contains("invoke=\"onCardAuditInfoClick\""));
    }

    @Test
    public void mainTabStacksPersonalAndProfessionalDataVertically() throws IOException {
        String xml = readProjectFile(XML);

        // Контент вкладки «Основное» — вертикальный vbox, карточки друг над другом.
        assertTrue(xml.contains("<vbox id=\"jobCandidateMainSectionContent\""));
        assertTrue("Контейнер вкладки «Основное» должен быть vbox, а не hbox",
                !xml.contains("<hbox id=\"jobCandidateMainSectionContent\""));

        assertOrdered(xml,
                "id=\"jobCandidateMainSectionContent\"",
                "id=\"personalDataBlock\"",
                "id=\"professionalDataBlock\"");

        // Класс half-card (горизонтальная пара) в пределах вкладки «Основное» удалён
        // (во вкладке «Контакты» он используется контактными карточками легально).
        String mainTab = section(xml,
                "id=\"jobCandidateMainSectionContent\"",
                "<!-- TAB: Контакты -->");
        assertTrue("Вертикальная раскладка «Основного» не использует half-card",
                !mainTab.contains("job-candidate-half-card"));
    }

    @Test
    public void everyThemeContainsLocalLayoutGuards() throws IOException {
        for (String theme : THEMES) {
            String scss = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/job-candidate-editor.scss");

            assertTrue(theme, scss.contains(".job-candidate-tabs > .v-tabsheet-tabcontainer"));
            assertTrue(theme, scss.contains(".v-slot-job-candidate-sidebar"));
            assertTrue(theme, scss.contains("max-width: 112px"));
            assertTrue(theme, scss.contains("font-size: 13px !important"));
            assertTrue(theme, scss.contains("white-space: nowrap !important"));
            assertTrue(theme, scss.contains("text-overflow: ellipsis"));
            assertTrue(theme, scss.contains(".job-candidate-position-column"));
            assertTrue(theme, scss.contains("overflow-x: auto"));
            assertTrue(theme, scss.contains(".job-candidate-social-actions"));
            assertTrue(theme, scss.contains("width: 220px !important"));
        }
    }

    @Test
    public void jobCandidateEditorScssIsIdenticalAcrossAllThemes() throws IOException {
        String canon = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/job-candidate-editor.scss");
        for (String theme : THEMES) {
            String scss = readProjectFile(
                    "modules/web/themes/" + theme + "/com.company.hunttech/job-candidate-editor.scss");
            assertTrue("job-candidate-editor.scss не идентичен в теме " + theme, canon.equals(scss));
        }
    }

    @Test
    public void everyThemeAppliesJobCandidateThemeOutsideStarMixin() throws IOException {
        for (String theme : THEMES) {
            String ext = readProjectFile(
                    "modules/web/themes/" + theme + "/com.company.hunttech/" + theme + "-ext.scss");
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");

            // Артефакт прошлого рефакторинга: include, застрявший внутри @mixin star
            // (mixin star нигде не вызывается — стили формы не применялись).
            assertTrue(!ext.contains("job-candidate-editor-theme;content"));

            // Общий screen-specific mixin применяется ровно один раз — либо в ext,
            // либо в styles.scss темы (эталон hunttech-modern).
            assertTrue(theme, ext.contains("job-candidate-editor-theme")
                    || styles.contains("job-candidate-editor-theme"));
        }
    }

    private String section(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        assertTrue("Не найден начальный маркер: " + startMarker, start >= 0);
        int end = text.indexOf(endMarker, start);
        assertTrue("Не найден конечный маркер: " + endMarker, end > start);
        return text.substring(start, end);
    }

    private void assertOrdered(String text, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int current = text.indexOf(marker);
            assertTrue("Не найден маркер: " + marker, current >= 0);
            assertTrue("Нарушен порядок: " + marker, current > previous);
            previous = current;
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
