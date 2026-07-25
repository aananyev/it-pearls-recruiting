package com.company.hunttech.core;

import org.junit.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает UI-контракт аккордеонов и кликабельной навигации IteractionListEdit.
 */
public class IteractionListAccordionNavigationTest {

    private static final List<String> THEMES = Arrays.asList(
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark");

    @Test
    public void controllerExtendsBaseScreenAndCreatesFiveNavigationButtons() throws Exception {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/"
                        + "IteractionListEditAccordionNavigation.java");

        assertTrue(controller.contains("@UiController(\"hunttech_IteractionList.edit\")"));
        assertTrue(controller.contains("extends IteractionListEdit"));
        assertTrue(controller.contains("iteraction-list-edit-accordion-navigation.xml"));
        assertTrue(controller.contains("iteractionListNavigation.removeAll()"));
        assertTrue(controller.contains("participantsAccordionNav"));
        assertTrue(controller.contains("interactionAccordionNav"));
        assertTrue(controller.contains("resultAccordionNav"));
        assertTrue(controller.contains("commentAccordionNav"));
        assertTrue(controller.contains("popularAccordionNav"));
        assertTrue(controller.contains("borderless iteraction-list-nav-item"));
    }

    @Test
    public void navigationSelectsExactlyOneAccordionAndSynchronizesHeaderClicks() throws Exception {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/"
                        + "IteractionListEditAccordionNavigation.java");

        assertTrue(controller.contains("participantsAccordion.setExpanded(participantsAccordion == selectedAccordion)"));
        assertTrue(controller.contains("interactionAccordion.setExpanded(interactionAccordion == selectedAccordion)"));
        assertTrue(controller.contains("resultAccordion.setExpanded(resultAccordion == selectedAccordion)"));
        assertTrue(controller.contains("commentAccordion.setExpanded(commentAccordion == selectedAccordion)"));
        assertTrue(controller.contains("popularAccordion.setExpanded(popularAccordion == selectedAccordion)"));
        assertEquals(5, count(controller, "addExpandedStateChangeListener"));
        assertTrue(controller.contains("candidateField::focus"));
        assertTrue(controller.contains("iteractionTypeField::focus"));
        assertTrue(controller.contains("ratingField::focus"));
        assertTrue(controller.contains("commentField::focus"));
    }

    @Test
    public void descriptorContainsFiveAccordionsAndPreservesCriticalContracts() throws Exception {
        Path descriptorPath = projectRoot().resolve(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/"
                        + "iteraction-list-edit-accordion-navigation.xml");
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(descriptorPath.toFile());
        String descriptor = new String(Files.readAllBytes(descriptorPath), StandardCharsets.UTF_8);

        assertTrue(descriptor.contains("id=\"participantsAccordion\""));
        assertTrue(descriptor.contains("id=\"interactionAccordion\""));
        assertTrue(descriptor.contains("id=\"resultAccordion\""));
        assertTrue(descriptor.contains("id=\"commentAccordion\""));
        assertTrue(descriptor.contains("id=\"popularAccordion\""));
        assertEquals(6, count(descriptor, "collapsable=\"true\""));

        assertTrue(descriptor.contains("<ovaFallbackImage id=\"candidateImage\""));
        assertTrue(descriptor.contains("fallbackThemePath=\"icons/no-programmer.jpeg\""));
        assertTrue(descriptor.contains("id=\"candidateField\""));
        assertTrue(descriptor.contains("id=\"vacancyFiels\""));
        assertTrue(descriptor.contains("id=\"iteractionTypeField\""));
        assertTrue(descriptor.contains("id=\"ratingField\""));
        assertTrue(descriptor.contains("id=\"recrutierField\""));
        assertTrue(descriptor.contains("id=\"commentField\""));
        assertTrue(descriptor.contains("id=\"mostPopularHbox\""));
        assertTrue(descriptor.contains("id=\"mostPopularIteractionHBox\""));
        assertTrue(descriptor.contains("invoke=\"callActionEntity\""));
        assertTrue(descriptor.contains("invoke=\"onButtonSubscribeClick\""));
        assertTrue(descriptor.contains("action=\"windowCommitAndClose\""));
        assertTrue(descriptor.contains("action=\"windowClose\""));
        assertTrue(descriptor.contains("view=\"iteractionList-edit-view\""));
        assertTrue(descriptor.contains("e.iteractionTree.number like :number"));
        assertTrue(descriptor.contains("where k.reacrutier = :subscriber"));
    }

    @Test
    public void navigationDoesNotChangeBusinessOrDataState() throws Exception {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/"
                        + "IteractionListEditAccordionNavigation.java");

        assertFalse(controller.contains("setValue("));
        assertFalse(controller.contains("getEditedEntity()"));
        assertFalse(controller.contains("dataManager"));
        assertFalse(controller.contains("commit()"));
        assertFalse(controller.contains("openPositionsDl"));
        assertFalse(controller.contains("iteractionTypesLc"));
    }

    @Test
    public void allThemesIncludeOnlyLocalAccordionNavigationScss() throws Exception {
        for (String theme : THEMES) {
            String scss = readProjectFile(
                    "modules/web/themes/" + theme
                            + "/com.company.hunttech/iteraction-list-accordion-navigation.scss");
            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");

            assertTrue(scss.contains("@mixin iteraction-list-accordion-navigation-theme"));
            assertTrue(scss.contains(".iteraction-list-editor"));
            assertFalse(scss.contains("@mixin iteraction-list-accordion-navigation-theme {\n  .v-button"));
            assertTrue(styles.contains("@import \"com.company.hunttech/iteraction-list-accordion-navigation\";"));
            assertTrue(styles.contains("@include iteraction-list-accordion-navigation-theme;"));
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
