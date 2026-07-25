package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает годовой контракт пяти быстрых кнопок взаимодействий.
 */
public class IteractionListMostPopularInteractionTest {

    @Test
    public void queryUsesCurrentUserAndRollingYear() throws IOException {
        String controller = controller();

        assertTrue(controller.contains("POPULAR_INTERACTION_BUTTONS = 5"));
        assertTrue(controller.contains("calendar.add(Calendar.YEAR, -1)"));
        assertTrue(controller.contains("e.recrutier = :user"));
        assertTrue(controller.contains(
                "e.dateIteraction between :periodStart and :periodEnd"));
        assertTrue(controller.contains(
                ".parameter(\"user\", userSession.getUser())"));
    }

    @Test
    public void exactlyFiveEqualButtonsAreCreated() throws IOException {
        String controller = controller();

        assertTrue(controller.contains("index < POPULAR_INTERACTION_BUTTONS"));
        assertTrue(controller.contains("mostPopularHbox.removeAll()"));
        assertTrue(controller.contains("popularButton.setWidth(\"100%\")"));
        assertTrue(controller.contains("mostPopularHbox.expand(popularButton)"));
        assertFalse(controller.contains("toArray(new Component[0])"));
        assertTrue(controller.contains("configureEmptyPopularButton"));
    }

    @Test
    public void clickAssignsExactInteractionWithoutCaptionParsing() throws IOException {
        String controller = controller();

        assertTrue(controller.contains("iteractionTypeField.setValue(interaction)"));
        assertTrue(controller.contains("iteractionTypeField.focus()"));
        assertFalse(controller.contains("getCaption().substring"));
        assertFalse(controller.contains("setCaptionAsHtml(true)"));
    }

    @Test
    public void popularHostIsReachableFromWorkspaceNavigation() throws IOException {
        String descriptor = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        String workspace = descriptor.substring(descriptor.indexOf("id=\"iteractionListWorkspace\""));

        assertTrue(workspace.contains("id=\"iteractionListNavigation\""));
        assertTrue(workspace.contains("value=\"msg://mshMostPopular\""));
        assertTrue(workspace.contains("id=\"popularAccordion\""));
        assertTrue(workspace.contains("id=\"mostPopularHbox\""));
        assertTrue(workspace.contains("height=\"AUTO\""));
    }

    @Test
    public void scssProtectsVisibleFiveButtonGeometry() throws IOException {
        String scss = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/"
                        + "iteraction-list-accordion-navigation.scss");

        assertTrue(scss.contains("min-height: 52px"));
        assertTrue(scss.contains("max-width: 20% !important"));
        assertTrue(scss.contains(".iteraction-list-popular-button"));
    }

    private String controller() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java");
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
