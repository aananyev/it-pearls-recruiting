package com.company.hunttech.core;

import org.junit.Test;

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
 * Защищает label-навигацию четырёх постоянных блоков и отсутствие
 * бизнес-действий в её presentation-участке активного controller.
 */
public class IteractionListAccordionNavigationTest {

    @Test
    public void activeControllerCreatesFourButtonsAndTogglesOnlyPresentationStyles()
            throws IOException {
        String presentation = activePresentation();

        assertTrue(presentation.contains("NAVIGATION_STYLE"));
        assertTrue(presentation.contains("ACTIVE_NAVIGATION_STYLE"));
        assertTrue(presentation.contains("ACTIVE_SECTION_STYLE"));
        assertEquals(4, count(presentation, "createNavigationButton(" ) - 1);
        assertEquals(4, count(presentation, "::focus"));
        assertTrue(presentation.contains("section.addStyleName(ACTIVE_SECTION_STYLE)"));
        assertTrue(presentation.contains("section.removeStyleName(ACTIVE_SECTION_STYLE)"));
        assertFalse(presentation.contains("setExpanded("));
        assertFalse(presentation.contains("addExpandedStateChangeListener"));
        assertFalse(presentation.contains("synchronizeExpandedAccordion"));
        assertFalse(presentation.contains("popularAccordionNav"));
    }

    @Test
    public void navigationUsesFourExpectedFocusTargets() throws IOException {
        String presentation = activePresentation();

        assertTrue(presentation.contains("candidateField::focus"));
        assertTrue(presentation.contains("iteractionTypeField::focus"));
        assertTrue(presentation.contains("ratingField::focus"));
        assertTrue(presentation.contains("commentField::focus"));
        assertTrue(presentation.contains("selectSection(participantsAccordion"));
        assertTrue(presentation.contains("selectSection(interactionAccordion"));
        assertTrue(presentation.contains("selectSection(resultAccordion"));
        assertTrue(presentation.contains("selectSection(commentAccordion"));
    }

    @Test
    public void navigationPresentationDoesNotWriteEntityRunLoadersOrCommit() throws IOException {
        String presentation = activePresentation();

        assertFalse(presentation.contains("DataManager"));
        assertFalse(presentation.contains("InteractionService"));
        assertFalse(presentation.contains("getEditedEntity()"));
        assertFalse(presentation.contains("commit("));
        assertFalse(presentation.contains(".load("));
        assertFalse(presentation.contains("setValue("));
    }

    private String descriptor() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
    }

    private String activePresentation() throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java");
        return section(
                controller,
                "private static final int POPULAR_INTERACTION_BUTTONS",
                "private static final String QUERY_CHAIN_LAST");
    }

    private String section(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        assertTrue("Не найден начальный маркер: " + startMarker, start >= 0);
        int end = text.indexOf(endMarker, start);
        assertTrue("Не найден конечный маркер: " + endMarker, end > start);
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
