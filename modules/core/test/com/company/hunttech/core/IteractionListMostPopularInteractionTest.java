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
 * Неизменяемый регрессионный контракт быстрых взаимодействий IteractionListEdit.
 * Визуальные рефакторинги формы не вправе менять период, пользователя, сервис
 * ранжирования или способ назначения выбранного Iteraction.
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
        assertTrue(service.contains("maxCount = list.size()"));
        assertFalse(service.contains("Calendar.YEAR"));
    }

    @Test
    public void formCreatesUpToFiveEqualGreenButtons() throws IOException {
        String controller = controller();

        assertTrue(controller.contains(
                "int buttonCount = Math.min(POPULAR_INTERACTION_BUTTONS, mostPopular.size())"));
        assertTrue(controller.contains("index < buttonCount"));
        assertTrue(controller.contains(
                "popularButton.setStyleName(\"iteraction-list-popular-button\")"));
        assertTrue(controller.contains("popularButton.setWidth(\"100%\")"));
        assertTrue(controller.contains("mostPopularHbox.removeAll()"));
        assertTrue(controller.contains("mostPopularHbox.expand(popularButton)"));
        assertFalse(controller.contains("configureEmptyPopularButton"));
        assertFalse(controller.contains("Нет данных"));
        assertFalse(controller.contains("getCaption().substring"));
    }

    @Test
    public void clickAssignsExactInteractionThroughStandardFieldHandler() throws IOException {
        String controller = controller();

        assertTrue(controller.contains("iteractionTypeField.setValue(interaction)"));
        assertTrue(controller.contains("iteractionTypeField.focus()"));
        assertTrue(controller.contains("@Subscribe(\"iteractionTypeField\")"));
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

        assertTrue(quickActions >= 0);
        assertTrue(popularHost > quickActions);
        assertTrue(participantsAccordion > popularHost);
        assertTrue(workspace.contains("id=\"mostPopularIteractionHBox\""));
        assertEquals(1, count(workspace, "id=\"mostPopularHbox\""));
        assertFalse(section(workspace,
                "id=\"mostPopularQuickActions\"",
                "id=\"participantsAccordion\"").contains("visible=\"false\""));
    }

    @Test
    public void scssKeepsQuickActionsVisibleAndEqual() throws IOException {
        String scss = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/"
                        + "iteraction-list-reference-finish.scss");

        assertTrue(scss.contains("min-height: 40px"));
        assertTrue(scss.contains("width: 20% !important"));
        assertTrue(scss.contains("height: 40px !important"));
        assertTrue(scss.contains("border-radius: 8px !important"));
        assertTrue(scss.contains(".iteraction-list-popular-button .v-button-caption"));
        assertTrue(scss.contains("visibility: visible !important"));
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
