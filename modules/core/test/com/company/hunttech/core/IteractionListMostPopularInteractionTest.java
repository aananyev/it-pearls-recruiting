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
 * Защищает исторический бизнес-контракт пяти быстрых взаимодействий.
 * Визуальный рефакторинг не вправе менять пользователя, месячный период,
 * ранжирование или назначение точного объекта Iteraction.
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
        assertTrue(service.contains("if (maxCount > list.size())"));
        assertFalse(service.contains("Calendar.YEAR"));
    }

    @Test
    public void fiveVisualPositionsPreserveRealButtonsAndDisableEmptySlots() throws IOException {
        String controller = controller();
        String presentation = presentation();

        // Базовый контроллер сохраняет фактический Iteraction внутри listener.
        assertTrue(controller.contains(
                "int buttonCount = Math.min(POPULAR_INTERACTION_BUTTONS, mostPopular.size())"));
        assertTrue(controller.contains("Iteraction interaction = mostPopular.get(index)"));
        assertTrue(controller.contains("iteractionTypeField.setValue(interaction)"));
        assertTrue(controller.contains("iteractionTypeField.focus()"));
        assertFalse(controller.contains("getCaption().substring"));
        assertFalse(controller.contains("QUERY_MOST_POPULAR"));

        // Presentation-слой добавляет только недостающие disabled-позиции.
        assertTrue(presentation.contains(
                "while (visiblePosition < POPULAR_INTERACTION_BUTTONS)"));
        assertTrue(presentation.contains("emptyButton.setCaption(EMPTY_POPULAR_CAPTION)"));
        assertTrue(presentation.contains("emptyButton.setEnabled(false)"));
        assertTrue(presentation.contains("mostPopularHbox.expand(emptyButton)"));
        assertFalse(presentation.contains("InteractionService"));
        assertFalse(presentation.contains("setValue("));
        String normalization = section(
                presentation,
                "private void normalizePopularButtons()",
                "\n    }\n}");
        assertFalse(normalization.contains("addClickListener"));
    }

    @Test
    public void quickActionsStayBetweenToolbarAndAccordionScrollArea() throws IOException {
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
        assertFalse(section(
                workspace,
                "id=\"mostPopularQuickActions\"",
                "id=\"iteractionListContentScrollBox\"").contains("visible=\"false\""));
        assertFalse(workspace.contains("id=\"popularAccordionNav\""));
    }

    @Test
    public void localScssKeepsFiveEqualReadablePositions() throws IOException {
        String scss = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/"
                        + "iteraction-list-reference-finish.scss");

        assertTrue(scss.contains("width: 20% !important"));
        assertTrue(scss.contains("height: 40px !important"));
        assertTrue(scss.contains(".iteraction-list-popular-button .v-button-caption"));
        assertTrue(scss.contains("white-space: normal"));
    }

    private String controller() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java");
    }

    private String presentation() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/"
                        + "IteractionListEditAccordionNavigation.java");
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
