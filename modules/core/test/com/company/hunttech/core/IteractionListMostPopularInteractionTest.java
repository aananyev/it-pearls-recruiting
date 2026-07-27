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
 * Защищает годовой контракт пяти быстрых кнопок взаимодействий
 * и их постоянное расположение внутри вкладки перед аккордеонами.
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
    public void popularButtonHostIsVisibleInsideTabBeforeFirstAccordion() throws IOException {
        String descriptor = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        String workspace = descriptor.substring(descriptor.indexOf("id=\"iteractionListWorkspace\""));
        int quickActions = workspace.indexOf("id=\"mostPopularQuickActions\"");
        int popularHost = workspace.indexOf("id=\"mostPopularHbox\"");
        int participantsAccordion = workspace.indexOf("id=\"participantsAccordion\"");
        int legacyPopularAccordion = workspace.indexOf("id=\"popularAccordion\"");

        assertTrue(quickActions >= 0);
        assertTrue(popularHost > quickActions);
        assertTrue(participantsAccordion > popularHost);
        assertTrue(legacyPopularAccordion > participantsAccordion);
        assertTrue(workspace.contains("id=\"mostPopularIteractionHBox\""));
        assertEquals(1, count(workspace, "id=\"mostPopularHbox\""));
        assertFalse(section(workspace,
                "id=\"mostPopularQuickActions\"",
                "id=\"participantsAccordion\"").contains("visible=\"false\""));
        assertTrue(workspace.substring(legacyPopularAccordion).contains("visible=\"false\""));
    }

    @Test
    public void scssProtectsVisibleFiveButtonGeometry() throws IOException {
        String scss = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/"
                        + "iteraction-list-reference-finish.scss");

        assertTrue(scss.contains("min-height: 40px"));
        assertTrue(scss.contains("width: 20% !important"));
        assertTrue(scss.contains("height: 40px !important"));
        assertTrue(scss.contains("border-radius: 8px !important"));
        assertTrue(scss.contains(".iteraction-list-popular-button .v-button-caption"));
        assertTrue(scss.contains("visibility: visible !important"));
        assertFalse(scss.contains("background: #2e7d32 !important"));
        assertFalse(scss.contains("border-radius: 999px !important"));
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
