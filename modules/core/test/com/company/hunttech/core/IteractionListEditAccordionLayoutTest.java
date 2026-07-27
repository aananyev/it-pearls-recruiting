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
 * Защищает точную двухпанельную компоновку IteractionListEdit с четырьмя
 * постоянными VBox-блоками и неизменными business bindings.
 */
public class IteractionListEditAccordionLayoutTest {

    private static final String[] THEMES = {
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark"
    };

    @Test
    public void descriptorParsesAndFollowsDesignedScreenOrder() throws Exception {
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
        assertFalse(descriptor.contains("<groupBox"));
        assertEquals(1, count(descriptor, "id=\"mostPopularHbox\""));
    }

    @Test
    public void workspaceContainsFourPermanentVBoxInputBlocks() throws IOException {
        String descriptor = descriptor();

        assertEquals(4, count(descriptor,
                "stylename=\"iteraction-list-flat-section"));
        assertEquals(4, count(descriptor,
                "stylename=\"iteraction-list-flat-section-title edit-card-title\""));
        assertEquals(4, count(descriptor,
                "stylename=\"iteraction-list-flat-section-body"));
        assertFalse(descriptor.contains("collapsable="));
        assertFalse(descriptor.contains("collapsed="));
        assertFalse(descriptor.contains("showAsPanel="));
        assertFalse(descriptor.contains("id=\"popularAccordion\""));
        assertTrue(descriptor.contains("iteraction-list-flat-section-active"));
        assertTrue(descriptor.contains("id=\"participantsAccordionContent\""));
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
    public void activeControllerUsesVBoxSectionsWithoutExpandedState() throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java");

        assertEquals(5, count(controller, "private VBoxLayout"));
        assertFalse(controller.contains("GroupBoxLayout"));
        assertFalse(controller.contains("setExpanded("));
        assertFalse(controller.contains("addExpandedStateChangeListener"));
        assertFalse(controller.contains("popularAccordionNav"));
        assertTrue(controller.contains("ACTIVE_SECTION_STYLE"));
        assertTrue(controller.contains("selectSection("));
    }

    @Test
    public void everyThemeUsesExactLocalFlatLayoutContract() throws IOException {
        for (String theme : THEMES) {
            String partial = readProjectFile(
                    "modules/web/themes/" + theme
                            + "/com.company.hunttech/iteraction-list-flat-layout.scss");
            assertTrue(theme, partial.contains("@mixin iteraction-list-flat-layout-theme"));
            assertTrue(theme, partial.contains(".iteraction-list-flat-section-header"));
            assertTrue(theme, partial.contains(".iteraction-list-flat-section-title"));
            assertTrue(theme, partial.contains(".iteraction-list-flat-section-body"));
            assertTrue(theme, partial.contains(".iteraction-list-flat-section-active"));
            assertTrue(theme, partial.contains(".iteraction-list-flat-section:focus-within"));
            assertTrue(theme, partial.contains("width: 312px !important"));
            assertFalse(theme, partial.contains(".v-panel-collapsed"));
            assertFalse(theme, partial.contains("nth-child(6)"));
            assertFalse(theme, partial.contains("\n  .v-panel {"));
            assertFalse(theme, partial.contains("\n  .v-button {"));

            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            assertTrue(theme, styles.contains(
                    "@import \"com.company.hunttech/iteraction-list-flat-layout\";"));
            assertTrue(theme, styles.contains(
                    "@include iteraction-list-flat-layout-theme;"));
        }
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
