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
 * Защищает исторический бизнес-контракт и ровно пять визуальных позиций
 * быстрых взаимодействий в активном IteractionListEdit.
 */
public class IteractionListMostPopularInteractionTest {

    @Test
    public void controllerDelegatesRankingToInteractionService() throws IOException {
        String controller = controller();

        assertTrue(controller.contains("POPULAR_INTERACTION_BUTTONS = 5"));
        assertTrue(controller.contains("private InteractionService interactionService"));
        assertTrue(controller.contains("interactionService.getMostPolularIteraction("));
        assertTrue(controller.contains(
                "userSession.getUser(), POPULAR_INTERACTION_BUTTONS"));
        assertFalse(controller.contains("calendar.add(Calendar.YEAR, -1)"));
        assertFalse(controller.contains("dataManager.loadValues(QUERY_MOST_POPULAR)"));
        assertFalse(controller.contains("private static final String QUERY_MOST_POPULAR"));
    }

    @Test
    public void serviceOwnsCurrentUserRollingMonthContract() throws IOException {
        String service = readProjectFile(
                "modules/core/src/com/company/hunttech/core/InteractionServiceBean.java");

        assertTrue(service.contains("gregorianCalendar.add(Calendar.MONTH, -1)"));
        assertTrue(service.contains("e.dateIteraction between :endDate and :startDate"));
        assertTrue(service.contains("e.recrutier = :user"));
        assertTrue(service.contains("group by e.iteractionType"));
        assertTrue(service.contains("order by count(e.iteractionType) desc"));
        assertTrue(service.contains(".view(\"iteraction-picker-view\")"));
        assertTrue(service.contains("Collectors.toMap(Iteraction::getId"));
        assertTrue(service.contains("Math.min(maxCount, list.size())"));
        assertFalse(service.contains("Calendar.YEAR"));
    }

    @Test
    public void activeControllerAlwaysBuildsFiveVisualPositions() throws IOException {
        String controller = controller();
        String builder = section(
                controller,
                "private void setMostPopularIteraction()",
                "@Subscribe\n    public void onAfterShow");

        assertTrue(builder.contains(
                "for (int index = 0; index < POPULAR_INTERACTION_BUTTONS; index++)"));
        assertTrue(builder.contains(
                "Iteraction interaction = index < mostPopular.size()"));
        assertTrue(builder.contains("createPopularInteractionButton(index, interaction)"));
        assertTrue(builder.contains("mostPopularHbox.add(popularButton)"));
        assertTrue(builder.contains("mostPopularHbox.expand(popularButton)"));
        assertTrue(builder.contains("mostPopular = Collections.emptyList()"));
    }

    @Test
    public void realButtonsPreserveExactInteractionAndEmptySlotsAreDisabled()
            throws IOException {
        String controller = controller();
        String factory = section(
                controller,
                "private Button createPopularInteractionButton(int index, Iteraction interaction)",
                "@Subscribe\n    public void onAfterShow");

        assertTrue(factory.contains("if (interaction == null)"));
        assertTrue(factory.contains("popularButton.setCaption(EMPTY_POPULAR_CAPTION)"));
        assertTrue(factory.contains("popularButton.setEnabled(false)"));
        assertTrue(factory.contains("return popularButton"));
        // Историческая подпись быстрой кнопки 2024 года: «N. Название типа».
        assertTrue(factory.contains(".append(index + 1)"));
        assertTrue(factory.contains(".append(\". \")"));
        assertTrue(factory.contains("interaction.getIterationName()"));
        assertTrue(factory.contains("iteractionTypeField.setValue(interaction)"));
        assertTrue(factory.contains("iteractionTypeField.focus()"));
        assertFalse(factory.contains("getCaption().substring"));

        String emptyBranch = section(
                factory,
                "if (interaction == null)",
                "popularButton.setDescription(interaction.getIterationName())");
        assertFalse(emptyBranch.contains("addClickListener"));
    }

    @Test
    public void quickActionsStayBetweenToolbarAndInputBlocks() throws IOException {
        String descriptor = descriptor();
        String workspace = descriptor.substring(descriptor.indexOf("id=\"iteractionListWorkspace\""));

        assertOrdered(workspace,
                "stylename=\"iteraction-list-toolbar edit-toolbar\"",
                "id=\"mostPopularQuickActions\"",
                "id=\"mostPopularHbox\"",
                "id=\"iteractionListContentScrollBox\"",
                "id=\"participantsAccordion\"",
                "id=\"editActions\"");
        assertEquals(1, count(workspace, "id=\"mostPopularHbox\""));
        assertFalse(workspace.contains("id=\"popularAccordion\""));
        assertFalse(workspace.contains("id=\"popularAccordionNav\""));
    }

    @Test
    public void localScssKeepsFiveEqualReadablePositions() throws IOException {
        String scss = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/"
                        + "iteraction-list-reference-finish.scss");

        assertTrue(scss.contains("width: 20% !important"));
        assertTrue(scss.contains("height: 64px !important"));
        assertTrue(scss.contains("max-height: 48px"));
        assertTrue(scss.contains(".iteraction-list-popular-button .v-button-caption"));
        assertTrue(scss.contains("white-space: normal"));
        assertTrue(scss.contains("visibility: visible !important"));
        assertTrue(scss.contains("$v-font-color, $v-panel-background-color, 82%"));
        // Исторический зелёный стиль быстрых кнопок 2024 года.
        assertTrue(scss.contains("background: #008000 !important"));
        assertTrue(scss.contains("color: #ffffff !important"));
        assertTrue(scss.contains("rgba(81, 255, 0"));
        assertTrue(scss.contains("border-radius: 10px !important"));
    }

    private String controller() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java");
    }

    private String descriptor() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
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
