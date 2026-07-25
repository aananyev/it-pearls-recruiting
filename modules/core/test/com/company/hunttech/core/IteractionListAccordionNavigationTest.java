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
 * Защищает индекс пяти разделов IteractionListEdit в постоянной sidebar.
 */
public class IteractionListAccordionNavigationTest {

    @Test
    public void navigationHostIsImmediatelyBelowImagesBeforeContextCards() throws IOException {
        String descriptor = descriptor();
        String sidebar = section(descriptor,
                "stylename=\"iteraction-list-sidebar\"",
                "id=\"iteractionListWorkspace\"");

        // Индекс должен быть первым рабочим элементом после изображений, чтобы пользователь
        // видел навигацию до длинных служебных карточек при любой высоте окна.
        assertOrdered(sidebar,
                "stylename=\"iteraction-list-profile-header\"",
                "stylename=\"iteraction-list-identity-images\"",
                "id=\"iteractionListNavigation\"",
                "stylename=\"iteraction-list-sidebar-card iteraction-list-service-card\"",
                "stylename=\"iteraction-list-sidebar-card iteraction-list-vacancy-card\"",
                "id=\"iteractionListSidebarSpacer\"");
        assertEquals(5, count(sidebar, "stylename=\"iteraction-list-nav-item"));
        assertFalse(sidebar.contains("value=\"msg://msgHeaderIteraction\""));
        assertFalse(sidebar.contains("value=\"msg://msgIteractionList\""));

        String workspace = section(descriptor,
                "id=\"iteractionListWorkspace\"",
                "id=\"editActions\"");
        assertFalse(workspace.contains("id=\"iteractionListNavigation\""));
        assertFalse(workspace.contains("id=\"iteractionListSectionLayout\""));
    }

    @Test
    public void defaultControllerCreatesFiveNavigationButtons() throws IOException {
        String controller = controller();

        assertTrue(controller.contains("@UiController(\"hunttech_IteractionList.edit\")"));
        assertTrue(controller.contains("iteractionListNavigation.removeAll()"));
        assertTrue(controller.contains("participantsAccordionNav"));
        assertTrue(controller.contains("interactionAccordionNav"));
        assertTrue(controller.contains("resultAccordionNav"));
        assertTrue(controller.contains("commentAccordionNav"));
        assertTrue(controller.contains("popularAccordionNav"));
        assertEquals(5, count(controller, "addExpandedStateChangeListener"));
    }

    @Test
    public void navigationFocusesFirstFieldOfEachInputBlock() throws IOException {
        String controller = controller();

        assertTrue(controller.contains("candidateField::focus"));
        assertTrue(controller.contains("iteractionTypeField::focus"));
        assertTrue(controller.contains("ratingField::focus"));
        assertTrue(controller.contains("commentField::focus"));
        assertTrue(controller.contains(
                "popularAccordion.setExpanded(popularAccordion == selectedAccordion)"));
    }

    @Test
    public void navigationMethodsDoNotWriteEntityOrRunQueries() throws IOException {
        String controller = controller();
        String navigation = controller.substring(
                controller.indexOf("private void initAccordionNavigation()"),
                controller.indexOf("private static final String QUERY_CHAIN_LAST"));

        assertFalse(navigation.contains("getEditedEntity()"));
        assertFalse(navigation.contains("dataManager"));
        assertFalse(navigation.contains("commit("));
        assertFalse(navigation.contains("setValue("));
    }

    @Test
    public void compatibilityControllerIsThinAliasWithUniqueId() throws IOException {
        String alias = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/"
                        + "IteractionListEditAccordionNavigation.java");

        assertTrue(alias.contains(
                "@UiController(\"hunttech_IteractionList.edit.accordion\")"));
        assertTrue(alias.contains("@UiDescriptor(\"iteraction-list-edit.xml\")"));
        assertTrue(alias.contains("extends IteractionListEdit"));
        assertFalse(alias.contains("@Subscribe"));
    }

    private String descriptor() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
    }

    private String controller() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java");
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
            assertTrue("Не найден XML-маркер: " + marker, current >= 0);
            assertTrue("Нарушен порядок XML-маркера: " + marker, current > previous);
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
        return new String(Files.readAllBytes(projectRoot().resolve(relativePath)), StandardCharsets.UTF_8);
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
