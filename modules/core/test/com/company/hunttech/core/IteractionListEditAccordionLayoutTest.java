package com.company.hunttech.core;

import org.junit.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает runtime-видимость селекторов, пяти аккордеонов и основных picker-полей.
 */
public class IteractionListEditAccordionLayoutTest {

    @Test
    public void defaultDescriptorContainsWorkspaceSelectorAndFiveAccordions() throws Exception {
        Path descriptorPath = projectRoot().resolve(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(descriptorPath.toFile());
        String descriptor = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");

        assertOrdered(descriptor,
                "id=\"iteractionListWorkspace\"",
                "id=\"iteractionListSectionLayout\"",
                "id=\"iteractionListNavigation\"",
                "id=\"iteractionListContentScrollBox\"",
                "id=\"participantsAccordion\"",
                "id=\"interactionAccordion\"",
                "id=\"resultAccordion\"",
                "id=\"commentAccordion\"",
                "id=\"popularAccordion\"");
        assertTrue(descriptor.contains("id=\"mostPopularHbox\""));
    }

    @Test
    public void candidateAndVacancyUseTwoExplicitFlexColumns() throws IOException {
        String descriptor = descriptor();
        String participants = section(
                descriptor,
                "id=\"participantsAccordion\"",
                "id=\"interactionAccordion\"");

        assertEquals(2, count(participants, "<column flex=\"1\"/>"));
        assertEquals(2, count(participants, "stylename=\"iteraction-list-primary-picker\""));
        assertOrdered(participants, "id=\"candidateField\"", "id=\"vacancyFiels\"");
        assertTrue(participants.contains("dataContainer=\"iteractionListDc\""));
        assertTrue(participants.contains("property=\"candidate\""));
        assertTrue(participants.contains("type=\"picker_lookup\""));
        assertTrue(participants.contains("type=\"picker_open\""));
    }

    @Test
    public void onlyFirstAccordionIsExpandedInitially() throws IOException {
        String descriptor = descriptor();

        assertTrue(section(descriptor,
                "id=\"participantsAccordion\"",
                "id=\"interactionAccordion\"").contains("collapsed=\"false\""));
        assertTrue(section(descriptor,
                "id=\"interactionAccordion\"",
                "id=\"resultAccordion\"").contains("collapsed=\"true\""));
        assertTrue(section(descriptor,
                "id=\"resultAccordion\"",
                "id=\"commentAccordion\"").contains("collapsed=\"true\""));
        assertTrue(section(descriptor,
                "id=\"commentAccordion\"",
                "id=\"popularAccordion\"").contains("collapsed=\"true\""));
        assertTrue(descriptor.substring(descriptor.indexOf("id=\"popularAccordion\""))
                .contains("collapsed=\"true\""));
    }

    @Test
    public void businessBindingsAndActionsRemainAvailable() throws IOException {
        String descriptor = descriptor();

        assertOrdered(descriptor,
                "id=\"candidateField\"",
                "id=\"vacancyFiels\"",
                "id=\"iteractionTypeField\"",
                "id=\"buttonsPanelCallAction\"",
                "id=\"ratingField\"",
                "id=\"recrutierField\"",
                "id=\"communicationMethodField\"",
                "id=\"commentField\"",
                "id=\"mostPopularHbox\"");
        assertTrue(descriptor.contains("invoke=\"callActionEntity\""));
        assertTrue(descriptor.contains("invoke=\"onButtonSubscribeClick\""));
        assertTrue(descriptor.contains("action=\"windowCommitAndClose\""));
    }

    @Test
    public void allThemesKeepGridInternalsUnderCubaControl() throws IOException {
        String[] themes = {
                "halo", "havana", "helium", "hover",
                "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark"
        };

        for (String theme : themes) {
            String scss = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/iteraction-list-accordion-navigation.scss");
            assertTrue(scss.contains(".iteraction-list-editor"));
            assertTrue(scss.contains(".iteraction-list-section-layout"));
            assertTrue(scss.contains("width: 210px !important"));
            assertTrue(scss.contains(".iteraction-list-primary-picker"));
            assertTrue(scss.contains("max-width: 20% !important"));
            assertFalse(scss.contains(".iteraction-list-form-grid > div"));
            assertFalse(scss.contains(".iteraction-list-form-grid table"));
        }
    }

    private String descriptor() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
    }

    private String section(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        assertTrue("Не найден начальный XML-маркер: " + startMarker, start >= 0);
        int end = text.indexOf(endMarker, start);
        assertTrue("Не найден конечный XML-маркер: " + endMarker, end > start);
        return text.substring(start, end);
    }

    private int count(String text, String token) {
        int result = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            result++;
            index += token.length();
        }
        return result;
    }

    private void assertOrdered(String descriptor, String... markers) {
        int previousIndex = -1;
        for (String marker : markers) {
            int currentIndex = descriptor.indexOf(marker);
            assertTrue("Не найден обязательный XML-маркер: " + marker, currentIndex >= 0);
            assertTrue("Нарушен порядок XML-маркера: " + marker, currentIndex > previousIndex);
            previousIndex = currentIndex;
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
