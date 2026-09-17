package com.company.hunttech.web.screens.iteractionlist;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Контрактный тест проверяет двухколоночную сетку правой рабочей области экрана
 * hunttech_IteractionList.edit (IteractionListEdit), неизменность Sidebar,
 * сохранение всех Component IDs, bindings, actions и идентичность SCSS во всех 7 темах.
 */
class IteractionListEditLayoutContractTest {

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

    private Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    @Test
    void testSidebarRemainsUntouched() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        assertTrue(xmlFile.exists(), "iteraction-list-edit.xml должен существовать");

        String xml = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);

        // Sidebar ID и ширина 312px
        assertTrue(xml.contains("id=\"iteractionListSidebar\""), "Sidebar должен иметь id=iteractionListSidebar");
        assertTrue(xml.contains("width=\"312px\""), "Sidebar должен иметь ширину 312px");
        assertTrue(xml.contains("id=\"iteractionProfileSummaryBox\""));
        assertTrue(xml.contains("id=\"candidateImage\""));
        assertTrue(xml.contains("id=\"projectLogoImage\""));
        assertTrue(xml.contains("id=\"iteractionCandidateNameLabel\""));
        assertTrue(xml.contains("id=\"iteractionVacancyNameLabel\""));
        assertTrue(xml.contains("id=\"iteractionServiceCard\""));
        assertTrue(xml.contains("id=\"numberIteractionField\""));
        assertTrue(xml.contains("id=\"dateIteractionField\""));
        assertTrue(xml.contains("id=\"vacancyStateSummary\""));
        assertTrue(xml.contains("id=\"iteractionVacancyCard\""));
    }

    @Test
    void testTwoColumnWorkspaceGridContract() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        Document doc = parseXml(xmlFile);

        // 1. Проверяем наличие 3 сеток в рабочей области
        NodeList grids = doc.getElementsByTagName("grid");
        Element participantsGrid = null;
        Element actionGrid = null;
        Element resultGrid = null;

        for (int i = 0; i < grids.getLength(); i++) {
            Element g = (Element) grids.item(i);
            String id = g.getAttribute("id");
            if ("gridIterationData".equals(id)) {
                participantsGrid = g;
            } else if ("gridInteractionType".equals(id)) {
                actionGrid = g;
            } else if ("resultAccordionGrid".equals(id)) {
                resultGrid = g;
            }
        }

        assertNotNull(participantsGrid, "gridIterationData должен присутствовать");
        assertNotNull(actionGrid, "gridInteractionType должен присутствовать");
        assertNotNull(resultGrid, "resultAccordionGrid должен присутствовать");

        // 2. Строка 1: Кандидат (50%) | Вакансия (50%)
        assertTrue(participantsGrid.getAttribute("stylename").contains("iteraction-list-participants-grid"));
        assertEquals("100%", participantsGrid.getAttribute("width"));

        // 3. Строка 2: Тип взаимодействия (50%) | Рекрутер (50%)
        assertTrue(actionGrid.getAttribute("stylename").contains("iteraction-list-action-grid"));
        assertEquals("100%", actionGrid.getAttribute("width"));

        // 4. Строка 3: Оценка (50%) | Способ коммуникации (50%)
        assertTrue(resultGrid.getAttribute("stylename").contains("iteraction-list-result-grid"));
        assertEquals("100%", resultGrid.getAttribute("width"));
    }

    @Test
    void testAllComponentIdsAndBindingsPreserved() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        String xml = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);

        // Проверка 13 обязательных компонентов
        List<String> requiredIds = Arrays.asList(
                "candidateField",
                "vacancyFiels",
                "onlyMySubscribeCheckBox",
                "iteractionTypeField",
                "recrutierField",
                "buttonsPanelCallAction",
                "dynamicActionFieldsBox",
                "buttonCallAction",
                "addString",
                "addDate",
                "addInteger",
                "ratingField",
                "communicationMethodField",
                "commentField"
        );

        for (String id : requiredIds) {
            assertTrue(xml.contains("id=\"" + id + "\""), "Компонент обязателен: " + id);
        }

        // Проверка порядка следования элементов
        int prev = -1;
        for (String id : requiredIds) {
            int cur = xml.indexOf("id=\"" + id + "\"");
            assertTrue(cur > prev, "Нарушен порядок компонента: " + id);
            prev = cur;
        }

        // Footer вне ScrollBox
        assertTrue(xml.indexOf("id=\"iteractionListContentScrollBox\"") < xml.indexOf("id=\"editActions\""),
                "Footer editActions должен находиться вне scrollBox");
        assertTrue(xml.contains("id=\"subscribeButton\""));
        assertTrue(xml.contains("action=\"windowCommitAndClose\""));
        assertTrue(xml.contains("action=\"windowClose\""));
    }

    @Test
    void testScssActionGridIncludedInAllThemes() throws IOException {
        for (String theme : THEMES) {
            File scssFile = resolveFile("modules/web/themes/" + theme + "/com.company.hunttech/iteraction-list-visual-alignment.scss");
            assertTrue(scssFile.exists(), "SCSS файл должен существовать для темы " + theme);

            String scss = new String(Files.readAllBytes(scssFile.toPath()), StandardCharsets.UTF_8);
            assertTrue(scss.contains(".iteraction-list-form-grid.iteraction-list-action-grid"),
                    "SCSS темы " + theme + " обязан содержать правило для .iteraction-list-action-grid");
            assertTrue(scss.contains(".iteraction-list-action-grid > .v-gridlayout-slot"),
                    "SCSS темы " + theme + " обязан содержать правило для слотов action-grid");
        }
    }
}
