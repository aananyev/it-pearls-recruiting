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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает исправление наложений IteractionListEdit и безопасную цепочку
 * отображения FileDescriptor через узкий view и фактическую проверку FileStorage.
 */
public class IteractionListLayoutStorageContractTest {

    private static final List<String> THEMES = Arrays.asList(
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark");

    @Test
    public void participantsLayoutKeepsEqualPickerRowAndSubscriptionBelowGrid() throws Exception {
        String xml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                projectRoot().resolve(
                        "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml")
                        .toFile());

        String participants = section(xml, "id=\"participantsAccordion\"", "id=\"interactionAccordion\"");
        assertTrue(participants.contains("height=\"AUTO\""));
        assertTrue(participants.contains("id=\"candidateField\""));
        assertTrue(participants.contains("id=\"vacancyFiels\""));
        assertTrue(participants.contains("id=\"onlyMySubscribeCheckBox\""));
        assertTrue(participants.contains("stylename=\"iteraction-list-subscription-filter\""));
        assertTrue(participants.indexOf("id=\"vacancyFiels\"")
                < participants.indexOf("</grid>"));
        assertTrue(participants.indexOf("</grid>")
                < participants.indexOf("id=\"onlyMySubscribeCheckBox\""));
        assertFalse(participants.contains("stylename=\"iteraction-list-vacancy-column\""));
    }

    @Test
    public void candidateSuggestionUsesNarrowImageSafeView() throws Exception {
        String xml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        String views = readProjectFile(
                "modules/global/src/com/company/hunttech/iteraction-list-views.xml");
        String component = readProjectFile(
                "modules/global/src/com/company/hunttech/app-component.xml");

        assertTrue(xml.contains("view=\"jobCandidate-iteraction-list-suggestion-view\""));
        assertTrue(component.contains("com/company/hunttech/iteraction-list-views.xml"));
        assertTrue(views.contains("name=\"jobCandidate-iteraction-list-suggestion-view\""));
        assertTrue(views.contains("name=\"fileImageFace\""));
        assertTrue(views.contains("name=\"personPosition\""));
        assertTrue(views.contains("name=\"cityOfResidence\""));
        assertFalse(views.contains("name=\"candidateCv\""));
        assertFalse(views.contains("name=\"iteractionList\""));
        assertFalse(views.contains("name=\"skillTree\""));
    }

    @Test
    public void imageFallbackValidatesReadableStreamAndHandlesUnfetchedValue() throws IOException {
        String helper = readProjectFile(
                "modules/web/src/com/company/hunttech/web/util/FileDescriptorImageHelper.java");
        String delegate = readProjectFile(
                "modules/web/src/com/hunttech/hrm/web/components/delegate/FallbackImageResourceDelegate.java");

        assertTrue(helper.contains("try (InputStream stream = fileLoader.openStream(fileDescriptor))"));
        assertTrue(helper.contains("catch (FileStorageException | IOException e)"));
        assertFalse(helper.contains("return fileLoader.fileExists(fileDescriptor)"));
        assertTrue(delegate.contains("value = valueSource.getValue()"));
        assertTrue(delegate.contains("catch (RuntimeException e)"));
        assertTrue(delegate.contains("host.updateValue(fallbackResource)"));
    }

    @Test
    public void allThemesKeepNaturalAccordionHeightAndVisibleSubscriptionRow() throws IOException {
        for (String theme : THEMES) {
            String scss = readProjectFile("modules/web/themes/" + theme
                    + "/com.company.hunttech/iteraction-list-reference-finish.scss");
            assertTrue(scss.contains(".v-slot-iteraction-list-accordion-section"));
            assertTrue(scss.contains("height: auto !important;"));
            assertTrue(scss.contains(".iteraction-list-participants-section .v-panel-content"));
            assertTrue(scss.contains("min-height: 126px !important;"));
            assertTrue(scss.contains(".iteraction-list-subscription-filter"));
            assertTrue(scss.contains("scroll-padding-top: 18px;"));
            assertFalse(scss.contains(".v-panel-content {\n      height: 70px"));
        }
    }

    private String section(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        assertTrue("Не найден начальный маркер: " + startMarker, start >= 0);
        int end = text.indexOf(endMarker, start);
        assertTrue("Не найден конечный маркер: " + endMarker, end > start);
        return text.substring(start, end);
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
