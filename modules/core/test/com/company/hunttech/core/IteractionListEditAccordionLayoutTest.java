package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Защищает presentation-only контракт аккордеонной одноколоночной компоновки IteractionListEdit.
 */
public class IteractionListEditAccordionLayoutTest {

    @Test
    public void accordionsReuseSettingsWindowContractAndFieldsUseOneColumn() throws IOException {
        String descriptor = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");

        assertTrue(descriptor.contains("stylename=\"iteraction-list-content user-ai-profile-editor\""));
        assertTrue(descriptor.contains("stylename=\"user-ai-profile-section iteraction-list-form-card\""));
        assertTrue(descriptor.contains("stylename=\"user-ai-profile-section iteraction-list-comment-card\""));
        assertTrue(descriptor.contains("stylename=\"user-ai-profile-section\""));
        assertTrue(descriptor.contains("margin=\"true\""));
        assertTrue(descriptor.contains("showAsPanel=\"true\""));
        assertFalse(descriptor.contains("iteraction-list-accordion-section"));
        assertTrue(descriptor.contains("<columns count=\"1\"/>"));
        assertTrue(descriptor.contains("id=\"gridIterationData\""));
    }

    @Test
    public void primarySectionIsExpandedAndSecondarySectionsAreCollapsed() throws IOException {
        String descriptor = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");

        assertOrdered(descriptor,
                "caption=\"msg://msgIteractionList\"",
                "caption=\"msg://msgComment\"",
                "caption=\"msg://mshMostPopular\"");

        String mainSection = descriptor.substring(
                descriptor.indexOf("caption=\"msg://msgIteractionList\""),
                descriptor.indexOf("caption=\"msg://msgComment\""));
        String commentSection = descriptor.substring(
                descriptor.indexOf("caption=\"msg://msgComment\""),
                descriptor.indexOf("caption=\"msg://mshMostPopular\""));
        String popularSection = descriptor.substring(
                descriptor.indexOf("caption=\"msg://mshMostPopular\""));

        assertTrue(mainSection.contains("collapsed=\"false\""));
        assertTrue(commentSection.contains("collapsed=\"true\""));
        assertTrue(popularSection.contains("collapsed=\"true\""));
    }

    @Test
    public void settingsWindowAccordionStyleExistsInAllThemes() throws IOException {
        String[] themes = {
                "halo",
                "havana",
                "helium",
                "hover",
                "hunttech-modern",
                "hunttech-modern-light",
                "hunttech-modern-dark"
        };

        for (String theme : themes) {
            String scss = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/user-ai-profile.scss");
            assertTrue("В теме " + theme + " отсутствует канонический аккордеон SettingsWindow",
                    scss.contains(".user-ai-profile-section"));
            assertTrue("В теме " + theme + " отсутствует оформление заголовка аккордеона",
                    scss.contains(".user-ai-profile-section .v-panel-caption"));
        }
    }

    @Test
    public void businessFieldsRemainInRecruiterWorkflowOrder() throws IOException {
        String descriptor = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");

        assertOrdered(descriptor,
                "id=\"candidateField\"",
                "id=\"vacancyFiels\"",
                "id=\"iteractionTypeField\"",
                "id=\"buttonsPanelCallAction\"",
                "id=\"ratingField\"",
                "id=\"recrutierField\"",
                "id=\"communicationMethodField\"",
                "id=\"commentField\"");
    }

    @Test
    public void bindingsAndActionsRemainUnchanged() throws IOException {
        String descriptor = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");

        assertTrue(descriptor.contains("property=\"candidate\""));
        assertTrue(descriptor.contains("property=\"vacancy\""));
        assertTrue(descriptor.contains("property=\"iteractionType\""));
        assertTrue(descriptor.contains("property=\"rating\""));
        assertTrue(descriptor.contains("property=\"recrutier\""));
        assertTrue(descriptor.contains("invoke=\"callActionEntity\""));
        assertTrue(descriptor.contains("invoke=\"onButtonSubscribeClick\""));
        assertTrue(descriptor.contains("action=\"windowCommitAndClose\""));
    }

    private void assertOrdered(String descriptor, String... markers) {
        int previousIndex = -1;
        for (String marker : markers) {
            int currentIndex = descriptor.indexOf(marker);
            assertTrue("Не найден обязательный XML-маркер: " + marker, currentIndex >= 0);
            assertTrue("Нарушен порядок XML-маркера: " + marker, currentIndex > previousIndex);
            previousIndex = currentIndex;
        }
    }

    private String readProjectFile(String relativePath) throws IOException {
        Path current = Paths.get("").toAbsolutePath();
        for (int level = 0; level < 8 && current != null; level++) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
            }
            current = current.getParent();
        }
        throw new IOException("Не найден файл проекта: " + relativePath);
    }
}
