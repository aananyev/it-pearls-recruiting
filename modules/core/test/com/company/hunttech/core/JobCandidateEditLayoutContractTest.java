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

        // Класс half-card (горизонтальная пара) удалён из формы целиком — мёртвый stylename
        // (P2-10 дизайн-ревью 2026-08-03); карточки «Контактов» держатся на job-candidate-card-row.
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
            assertTrue(theme, scss.contains("min-height: 27px !important"));
            assertTrue(theme, scss.contains("max-width: none !important"));
            assertTrue(theme, scss.contains("font-size: 14px !important"));
            assertTrue(theme, scss.contains("white-space: nowrap !important"));
            assertTrue(theme, scss.contains("text-overflow: clip !important"));
            assertTrue(theme, scss.contains(".job-candidate-position-column"));
            assertTrue(theme, scss.contains("overflow-x: auto !important"));
            assertTrue(theme, scss.contains(".job-candidate-social-actions"));
            assertTrue(theme, scss.contains("width: 220px !important"));
            assertTrue(theme, scss.contains(".c-fileupload-container"));
            assertTrue(theme, scss.contains(".c-fileupload-clear"));
            assertTrue(theme, scss.contains("width: 96px !important"));
            assertTrue(theme, scss.contains(".job-candidate-bottom-bar > .v-expand"));
            assertTrue(theme, scss.contains("justify-content: flex-end"));
            assertTrue(theme, scss.contains("flex: 0 0 auto !important"));
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

    @Test
    public void allTabsUseConsistentMargin() throws IOException {
        String xml = readProjectFile(XML);
        String tabs = section(xml, "<tabSheet id=\"tabSheetSocialNetworks\"", "</tabSheet>");
        assertTrue("Все вкладки должны иметь margin=\"false\" — единый отступ контента "
                        + "через SCSS .v-tabsheet-content (иначе accordion-заголовки в разных вкладках смещены)",
                !tabs.contains("margin=\"true\""));
    }

    @Test
    public void mainTabRowsShareExpandedFieldPattern() throws IOException {
        String xml = readProjectFile(XML);

        // Каждая строка ввода вкладки «Основное» использует hbox expand + width=100% поля:
        // колонка подписей (112/96px) и поле calc(100% - подпись) применяются ко ВСЕМ строкам,
        // а не только к полям ФИО (иначе «Дата рождения»/«Город»/«Должность»/«Компания»
        // имеют natural width подписей и поля иной ширины).
        assertTrue(xml.contains("expand=\"birdhDateField\""));
        assertTrue(xml.contains("expand=\"jobCityCandidateField\""));
        assertTrue(xml.contains("expand=\"personPositionField\""));
        assertTrue(xml.contains("expand=\"currentCompanyField\""));

        String dateRow = section(xml, "id=\"birdhDateField\"", "</hbox>");
        assertTrue(dateRow.contains("width=\"100%\""));
    }

    @Test
    public void commentsTabKeepsCleanChatLayout() throws IOException {
        String xml = readProjectFile(XML);
        String comments = section(xml, "id=\"commentsTab\"", "<!-- TAB: История -->");

        assertTrue("Лента комментариев — scrollBox + vbox (авто-высота пузырей)",
                comments.contains("id=\"jobCandidateCommentsScroll\""));
        assertTrue(comments.contains("id=\"jobCandidateCommentsContainer\""));
        assertTrue("dataGrid удалён: Vaadin Grid не поддерживает авто-высоту строк — "
                        + "пузыри разной высоты перекрывались (rowHeight 30px)",
                !comments.contains("<dataGrid"));
        assertTrue("Высота строки комментариев должна быть автоматической (по содержимому пузыря), "
                        + "а не фиксированной 80px", !comments.contains("bodyRowHeight"));
        assertTrue("JPQL ленты не должен содержать опечатку deteIteraction — она даёт "
                        + "JPQLException и пустую ленту (поле называется dateIteraction)",
                !comments.contains("deteIteraction"));
        assertTrue(comments.contains("inputPrompt=\"msg://msgInputComment\""));
    }

    @Test
    public void everyThemeStylesCommentBubbles() throws IOException {
        for (String theme : THEMES) {
            String scss = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/job-candidate-editor.scss");

            assertTrue(theme, scss.contains(".toolTip {"));
            assertTrue(theme, scss.contains(".tailMyMessage {"));
            assertTrue(theme, scss.contains(".tailOtherMessage {"));
            assertTrue(theme, scss.contains("max-width: 520px"));
            assertTrue(theme, scss.contains(".table-wordwrap {"));
        }
    }

    @Test
    public void additionalPositionsUseHorizontalFlowLayout() throws IOException {
        String xml = readProjectFile(XML);
        String grid = section(xml, "id=\"professionalDataGrid\"", "</grid>");

        // Список доп. позиций — горизонтальный flow-контейнер (cssLayout), а не Label:
        // значения выводятся в строку справа от кнопки «…» и переносятся по позициям,
        // а не по словам внутри одного Label (вертикальный столбик).
        assertTrue("Контейнер доп. позиций должен быть cssLayout (горизонтальный flow)",
                grid.contains("<cssLayout id=\"positionsLabel\""));
        assertTrue(grid.contains("width=\"100%\""));
        assertTrue(grid.contains("stylename=\"job-candidate-positions\""));
        assertTrue("Label доп. позиций должен быть заменён на cssLayout",
                !grid.contains("<label id=\"positionsLabel\""));
    }

    @Test
    public void everyThemeStylesPositionsFlowLocally() throws IOException {
        for (String theme : THEMES) {
            String scss = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/job-candidate-editor.scss");

            // Локальность: селектор ограничен namespace формы, не глобальный .v-label.
            assertTrue(theme, scss.contains(".job-candidate-positions {"));
            assertTrue(theme, scss.contains("display: flex"));
            assertTrue(theme, scss.contains("flex-wrap: wrap"));
            assertTrue(theme, scss.contains(".job-candidate-positions .v-label"));
            assertTrue(theme, scss.contains("white-space: nowrap"));
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
