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
 * Защищает label-навигацию четырёх реальных аккордеонов и отсутствие
 * бизнес-действий в presentation-контроллере IteractionListEdit.
 */
public class IteractionListAccordionNavigationTest {

    @Test
    public void descriptorContainsOnlyRealAccordionNavigationItems() throws IOException {
        String descriptor = descriptor();
        String sidebar = section(
                descriptor,
                "id=\"iteractionListNavigation\"",
                "stylename=\"iteraction-list-sidebar-card iteraction-list-service-card");

        assertTrue(sidebar.contains("stylename=\"label-navigation\""));
        assertTrue(sidebar.contains("stylename=\"label-nav-title\""));
        assertEquals(4, count(sidebar, "stylename=\"label-nav-item"));
        assertFalse(sidebar.contains("value=\"msg://mshMostPopular\""));
        assertFalse(sidebar.contains("popularAccordionNav"));
    }

    @Test
    public void runtimeNavigationKeepsBaseClassAndTogglesOnlyActiveState() throws IOException {
        String presentation = presentation();

        assertTrue(presentation.contains(
                "NAVIGATION_STYLE = \"borderless label-nav-item\""));
        assertTrue(presentation.contains(
                "ACTIVE_NAVIGATION_STYLE = \"label-nav-item-active\""));
        assertTrue(presentation.contains("button.addStyleName(\"label-nav-item\")"));
        assertTrue(presentation.contains(
                "button.addStyleName(ACTIVE_NAVIGATION_STYLE)"));
        assertTrue(presentation.contains(
                "button.removeStyleName(ACTIVE_NAVIGATION_STYLE)"));
        assertFalse(presentation.contains("iteraction-list-nav-item-active"));
    }

    @Test
    public void navigationUsesFourFocusTargetsAndSynchronizesManualExpansion() throws IOException {
        String presentation = presentation();

        assertTrue(presentation.contains("candidateField::focus"));
        assertTrue(presentation.contains("iteractionTypeField::focus"));
        assertTrue(presentation.contains("ratingField::focus"));
        assertTrue(presentation.contains("commentField::focus"));
        assertEquals(4, count(presentation, "addExpandedStateChangeListener"));
        assertTrue(presentation.contains("updatingAccordionState"));
        assertTrue(presentation.contains("synchronizeExpandedAccordion"));
        assertFalse(presentation.contains("focusFirstPopularButton"));
    }

    @Test
    public void presentationLayerDoesNotWriteEntityRunLoadersOrCommit() throws IOException {
        String presentation = presentation();

        assertFalse(presentation.contains("DataManager"));
        assertFalse(presentation.contains("InteractionService"));
        assertFalse(presentation.contains("getEditedEntity()"));
        assertFalse(presentation.contains("commit("));
        assertFalse(presentation.contains(".load("));
        assertFalse(presentation.contains("setValue("));
    }

    @Test
    public void legacyPresentationInitializerIsExplicitlyReplaced() throws IOException {
        String presentation = presentation();

        assertTrue(presentation.contains(
                "protected void onInitIteractionNavigation(InitEvent event)"));
        assertTrue(presentation.contains("@Override"));
        assertFalse(presentation.contains("popularNavigationButton"));
    }

    private String descriptor() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
    }

    private String presentation() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/"
                        + "IteractionListEditAccordionNavigation.java");
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
