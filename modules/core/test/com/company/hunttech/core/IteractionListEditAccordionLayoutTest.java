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
 * Защищает новую двухпанельную архитектуру IteractionListEdit: стандартный
 * HuntTech sidebar, отдельную панель быстрых действий, четыре независимых
 * бизнес-раздела и неизменные bindings/actions существующего controller-а.
 */
public class IteractionListEditAccordionLayoutTest {

    @Test
    public void descriptorParsesAndFollowsNewEditArchitecture() throws Exception {
        Path descriptorPath = descriptorPath();
        DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(descriptorPath.toFile());

        String descriptor = descriptor();
        assertOrdered(descriptor,
                "id=\"iteractionListMainLayout\"",
                "id=\"iteractionListSidebar\"",
                "id=\"iteractionListNavigation\"",
                "id=\"iteractionListWorkspace\"",
                "id=\"iteractionListToolbarBox\"",
                "id=\"mostPopularQuickActions\"",
                "id=\"mostPopularHbox\"",
                "id=\"iteractionListContentScrollBox\"",
                "id=\"participantsAccordion\"",
                "id=\"interactionAccordion\"",
                "id=\"resultAccordion\"",
                "id=\"commentAccordion\"",
                "id=\"editActions\"");

        assertFalse(descriptor.contains("id=\"iteractionMainInfoCard\""));
        assertFalse(descriptor.contains("id=\"iteractionMainInfoBody\""));
        assertFalse(descriptor.contains("<tabSheet"));
        assertFalse(descriptor.contains("<groupBox"));
        assertEquals(1, count(descriptor, "id=\"mostPopularHbox\""));
    }

    @Test
    public void previousDescriptorIsArchivedAsOldAndActiveDescriptorIsNew() throws IOException {
        Path oldDescriptor = projectRoot().resolve(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit-old.xml");
        assertTrue("Старый descriptor должен быть сохранён как old", Files.exists(oldDescriptor));

        String active = descriptor();
        String old = new String(Files.readAllBytes(oldDescriptor), StandardCharsets.UTF_8);
        assertTrue(active.contains("id=\"participantsAccordion\""));
        assertFalse(active.contains("id=\"iteractionMainInfoCard\""));
        assertTrue(old.contains("id=\"iteractionMainInfoCard\""));
    }

    @Test
    public void sidebarUsesHuntTechOrderIdentityNavigationThenContext() throws IOException {
        String descriptor = descriptor();

        assertTrue(descriptor.contains(
                "stylename=\"edit-sidebar iteraction-list-sidebar-v2\""));
        assertTrue(descriptor.contains("width=\"270px\""));
        assertOrdered(descriptor,
                "id=\"iteractionIdentityImages\"",
                "id=\"iteractionCandidateNameLabel\"",
                "id=\"iteractionVacancyNameLabel\"",
                "id=\"iteractionListNavigation\"",
                "id=\"iteractionServiceCard\"",
                "id=\"iteractionVacancyCard\"");

        assertTrue(descriptor.contains("stylename=\"label-navigation iteraction-list-navigation\""));
        assertTrue(descriptor.contains("id=\"participantsAccordionNav\""));
        assertTrue(descriptor.contains("id=\"interactionAccordionNav\""));
        assertTrue(descriptor.contains("id=\"resultAccordionNav\""));
        assertTrue(descriptor.contains("id=\"commentAccordionNav\""));
    }

    @Test
    public void candidateAndVacancyUseTwoEqualColumnsAndSeparateSubscriptionRow()
            throws IOException {
        String participants = section(
                descriptor(),
                "id=\"participantsAccordion\"",
                "id=\"interactionAccordion\"");

        assertEquals(2, count(participants, "<column width=\"50%\"/>"));
        assertOrdered(participants,
                "id=\"candidateField\"",
                "id=\"vacancyFiels\"",
                "id=\"onlyMySubscribeCheckBox\"");
        assertTrue(participants.contains("property=\"candidate\""));
        assertTrue(participants.contains("property=\"vacancy\""));
        assertTrue(participants.contains(
                "iteraction-list-form-grid iteraction-list-participants-grid"));
    }

    @Test
    public void businessBindingsActionsAndDynamicFieldsRemainAvailable() throws IOException {
        String descriptor = descriptor();

        assertOrdered(descriptor,
                "id=\"candidateField\"",
                "id=\"vacancyFiels\"",
                "id=\"iteractionTypeField\"",
                "id=\"buttonCallAction\"",
                "id=\"addString\"",
                "id=\"addDate\"",
                "id=\"addInteger\"",
                "id=\"ratingField\"",
                "id=\"recrutierField\"",
                "id=\"communicationMethodField\"",
                "id=\"commentField\"");

        assertTrue(descriptor.contains("invoke=\"callActionEntity\""));
        assertTrue(descriptor.contains("invoke=\"onButtonSubscribeClick\""));
        assertTrue(descriptor.contains("action=\"windowCommitAndClose\""));
        assertTrue(descriptor.contains("action=\"windowClose\""));
        assertTrue(descriptor.contains("optionsContainer=\"iteractionTypesDc\""));
        assertTrue(descriptor.contains("optionsContainer=\"openPositionDc\""));
        assertTrue(descriptor.contains("optionsContainer=\"usersDc\""));
    }

    @Test
    public void calendarDateFieldKeepsDateTimeBindingForCalendarInteractions() throws IOException {
        String interaction = section(
                descriptor(),
                "id=\"interactionAccordion\"",
                "id=\"resultAccordion\"");

        String addDate = section(interaction,
                "id=\"addDate\"",
                "id=\"addInteger\"");
        assertTrue(addDate.contains("property=\"addDate\""));
        assertTrue(addDate.contains("resolution=\"MIN\""));
        assertTrue(addDate.contains("dateFormat=\"dd.MM.yyyy HH:mm\""));
        assertTrue(addDate.contains("visible=\"false\""));

        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java");
        assertTrue(controller.contains("case 1:"));
        assertTrue(controller.contains("addDate.setVisible(true)"));
        assertTrue(controller.contains("addDate.setRequired(true)"));
        assertTrue(controller.contains("getSetDateTime()"));
        assertTrue(controller.contains("addDate.setValue(date)"));
    }

    @Test
    public void controllerKeepsBusinessLogicIndependentFromNewPresentationSections()
            throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java");

        assertTrue(controller.contains("interactionService.getMostPolularIteraction("));
        assertTrue(controller.contains("private void changeField()"));
        assertTrue(controller.contains("public void callActionEntity()"));
        assertTrue(controller.contains("onBeforeCommitChanges"));
        assertTrue(controller.contains("onAfterCommitChanges1"));
        assertFalse(controller.contains("GroupBoxLayout"));
        assertFalse(controller.contains("setExpanded("));
    }

    private String descriptor() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
    }

    private Path descriptorPath() {
        return projectRoot().resolve(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
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

    private void assertOrdered(String text, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int current = text.indexOf(marker);
            assertTrue("Не найден обязательный маркер: " + marker, current >= 0);
            assertTrue("Нарушен порядок маркера: " + marker, current > previous);
            previous = current;
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
