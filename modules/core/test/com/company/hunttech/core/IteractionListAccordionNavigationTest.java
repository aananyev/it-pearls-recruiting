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
 * Защищает label-навигацию IteractionListEdit и отсутствие бизнес-действий
 * в presentation-слое общего контракта Edit-экранов.
 */
public class IteractionListAccordionNavigationTest {

    @Test
    public void descriptorUsesCanonicalLabelNavigationNames() throws IOException {
        String descriptor = descriptor();
        String sidebar = section(descriptor,
                "stylename=\"iteraction-list-sidebar\"",
                "id=\"iteractionListWorkspace\"");

        assertOrdered(sidebar,
                "stylename=\"iteraction-list-profile-header\"",
                "id=\"iteractionCandidateNameLabel\"",
                "id=\"iteractionVacancyNameLabel\"",
                "id=\"iteractionListNavigation\"",
                "stylename=\"iteraction-list-sidebar-card iteraction-list-service-card\"");
        assertTrue(sidebar.contains("stylename=\"label-navigation\""));
        assertTrue(sidebar.contains("stylename=\"label-nav-title\""));
        assertEquals(5, count(sidebar, "stylename=\"label-nav-item"));
        assertFalse(sidebar.contains("stylename=\"iteraction-list-navigation\""));
    }

    @Test
    public void runtimeNavigationKeepsBaseClassAndTogglesOnlyActiveState() throws IOException {
        String extension = extension();

        assertTrue(extension.contains("NAVIGATION_STYLE = \"borderless label-nav-item\""));
        assertTrue(extension.contains("ACTIVE_NAVIGATION_STYLE = \"label-nav-item-active\""));
        assertTrue(extension.contains("button.addStyleName(\"label-nav-item\")"));
        assertTrue(extension.contains("button.addStyleName(ACTIVE_NAVIGATION_STYLE)"));
        assertTrue(extension.contains("button.removeStyleName(ACTIVE_NAVIGATION_STYLE)"));
        assertFalse(extension.contains("setStyleName(\"borderless iteraction-list-nav-item"));
    }

    @Test
    public void navigationUsesExistingSectionsAndMessageKeys() throws IOException {
        String extension = extension();

        assertTrue(extension.contains("messageBundle.getMessage(messageKey)"));
        assertTrue(extension.contains("candidateField::focus"));
        assertTrue(extension.contains("iteractionTypeField::focus"));
        assertTrue(extension.contains("ratingField::focus"));
        assertTrue(extension.contains("commentField::focus"));
        assertTrue(extension.contains("focusFirstPopularButton"));
        assertEquals(5, count(extension, "addExpandedStateChangeListener"));
    }

    @Test
    public void presentationAdapterDoesNotWriteEntityOrRunQueries() throws IOException {
        String extension = extension();

        assertFalse(extension.contains("DataManager"));
        assertFalse(extension.contains("InteractionService"));
        assertFalse(extension.contains("getEditedEntity()"));
        assertFalse(extension.contains("commit("));
        assertFalse(extension.contains("load("));
        assertFalse(extension.contains("setValue("));
    }

    private String descriptor() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
    }

    private String extension() throws IOException {
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

    private void assertOrdered(String text, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int current = text.indexOf(marker);
            assertTrue("Не найден маркер: " + marker, current >= 0);
            assertTrue("Нарушен порядок: " + marker, current > previous);
            previous = current;
        }
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
        return new String(Files.readAllBytes(projectRoot().resolve(relativePath)),
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
