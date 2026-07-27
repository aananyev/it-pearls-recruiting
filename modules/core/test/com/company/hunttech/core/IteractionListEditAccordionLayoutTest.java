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
 * Защищает заново спроектированную двухпанельную компоновку IteractionListEdit
 * и сохранность всех бизнес-компонентов после удаления промежуточного TabSheet.
 */
public class IteractionListEditAccordionLayoutTest {

    @Test
    public void descriptorParsesAndFollowsEditScreenOrder() throws Exception {
        Path descriptorPath = projectRoot().resolve(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(descriptorPath.toFile());

        String descriptor = descriptor();
        assertOrdered(descriptor,
                "id=\"iteractionListMainLayout\"",
                "stylename=\"iteraction-list-sidebar edit-sidebar\"",
                "id=\"iteractionListWorkspace\"",
                "stylename=\"iteraction-list-toolbar edit-toolbar\"",
                "id=\"mostPopularQuickActions\"",
                "id=\"mostPopularHbox\"",
                "id=\"iteractionListContentScrollBox\"",
                "id=\"participantsAccordion\"",
                "id=\"interactionAccordion\"",
                "id=\"resultAccordion\"",
                "id=\"commentAccordion\"",
                "id=\"editActions\"");
        assertFalse(descriptor.contains("<tabSheet"));
        assertFalse(descriptor.contains("id=\"iteractionListTabSheeet\""));
        assertEquals(1, count(descriptor, "id=\"mostPopularHbox\""));
    }

    @Test
    public void directSemanticClassesDefineTheScreenWithoutRuntimeStyleTraversal() throws IOException {
        String descriptor = descriptor();

        assertTrue(descriptor.contains(
                "stylename=\"iteraction-list-main-layout edit-screen-layout\""));
        assertTrue(descriptor.contains(
                "stylename=\"iteraction-list-workspace edit-workspace\""));
        assertTrue(descriptor.contains("edit-sidebar-visual"));
        assertTrue(descriptor.contains("edit-sidebar-identity"));
        assertTrue(descriptor.contains("edit-sidebar-title"));
        assertTrue(descriptor.contains("edit-sidebar-subtitle"));
        assertTrue(descriptor.contains("edit-sidebar-summary"));
        assertTrue(descriptor.contains("edit-workspace-scroll"));
        assertTrue(descriptor.contains("edit-workspace-content"));
        assertEquals(4, count(descriptor, "edit-accordion-section"));
        assertTrue(descriptor.contains("edit-footer-actions"));

        String presentation = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/"
                        + "IteractionListEditAccordionNavigation.java");
        assertFalse(presentation.contains("applySharedStyles"));
        assertFalse(presentation.contains("restoreProjectLogoRole"));
    }

    @Test
    public void candidateAndVacancyUseTwoEqualColumnsAndSeparateSubscriptionRow()
            throws IOException {
        String participants = section(
                descriptor(),
                "id=\"participantsAccordion\"",
                "id=\"interactionAccordion\"");

        assertEquals(2, count(participants, "<column flex=\"1\"/>"));
        assertOrdered(participants,
                "id=\"candidateField\"",
                "id=\"vacancyFiels\"",
                "id=\"onlyMySubscribeCheckBox\"");
        assertTrue(participants.contains("property=\"candidate\""));
        assertTrue(participants.contains("property=\"vacancy\""));
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
        assertTrue(descriptor.contains("required=\"true\""));
    }

    @Test
    public void accordionContractUsesNaturalHeightAndOneDefaultSection() throws IOException {
        String descriptor = descriptor();

        String participants = section(
                descriptor(),
                "id=\"participantsAccordion\"",
                "id=\"interactionAccordion\"");
        String interaction = section(
                descriptor(),
                "id=\"interactionAccordion\"",
                "id=\"resultAccordion\"");
        String result = section(
                descriptor(),
                "id=\"resultAccordion\"",
                "id=\"commentAccordion\"");
        String comment = section(
                descriptor(),
                "id=\"commentAccordion\"",
                "id=\"popularAccordion\"");

        assertTrue(participants.contains("height=\"AUTO\""));
        assertTrue(participants.contains("collapsed=\"false\""));
        assertTrue(interaction.contains("collapsed=\"true\""));
        assertTrue(result.contains("collapsed=\"true\""));
        assertTrue(comment.contains("collapsed=\"true\""));
    }

    @Test
    public void sharedContractPreventsHorizontalScrollAndDefinesResponsiveSidebar()
            throws IOException {
        String shared = readProjectFile(
                "modules/web/themes/common/edit-screen-shared-styles.scss");

        assertTrue(shared.contains("width: 270px !important"));
        assertTrue(shared.contains("@media (max-width: 1366px)"));
        assertTrue(shared.contains("width: 250px !important"));
        assertTrue(shared.contains("min-height: 58px"));
        assertTrue(shared.contains("min-height: 38px"));
        assertTrue(shared.contains("min-width: 0 !important"));
        assertTrue(shared.contains("overflow-x: hidden !important"));
        assertTrue(shared.contains("border-radius: 8px"));
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
