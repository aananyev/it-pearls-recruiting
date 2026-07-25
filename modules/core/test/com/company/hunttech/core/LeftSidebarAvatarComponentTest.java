package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает единый контракт профильных изображений в левых панелях HRM HuntTech.
 */
public class LeftSidebarAvatarComponentTest {

    @Test
    public void redesignedLeftSidebarsUseOvaFallbackImage() throws IOException {
        String jobCandidateXml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml");
        String settingsXml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml");
        String iteractionXml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");

        assertTrue(jobCandidateXml.contains("id=\"jobCandidateSidebar\""));
        assertTrue(jobCandidateXml.contains("<ovaFallbackImage id=\"candidatePic\""));
        assertFalse(jobCandidateXml.contains("<image id=\"candidatePic\""));

        assertTrue(settingsXml.contains("id=\"userAiProfileSidebar\""));
        assertTrue(settingsXml.contains("<ovaFallbackImage id=\"userPic\""));
        assertFalse(settingsXml.contains("<image id=\"userPic\""));

        assertTrue(iteractionXml.contains("stylename=\"iteraction-list-sidebar\""));
        assertTrue(iteractionXml.contains("<ovaFallbackImage id=\"candidateImage\""));
        assertFalse(iteractionXml.contains("<image id=\"candidateImage\""));
    }

    @Test
    public void iteractionSidebarPreservesFallbackAndControllerContract() throws IOException {
        String iteractionXml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java");

        /*
         * OvaFallbackImage наследует базовый CUBA Image, поэтому legacy-инъекция и
         * существующее runtime-обновление source/valueSource остаются совместимыми.
         */
        assertTrue(iteractionXml.contains("dataContainer=\"iteractionListDc\""));
        assertTrue(iteractionXml.contains("property=\"candidate.fileImageFace\""));
        assertTrue(iteractionXml.contains("ovalWidth=\"104px\""));
        assertTrue(iteractionXml.contains("ovalHeight=\"104px\""));
        assertTrue(iteractionXml.contains("fallbackThemePath=\"icons/no-programmer.jpeg\""));
        assertTrue(iteractionXml.contains("scaleMode=\"SCALE_DOWN\""));
        assertTrue(controller.contains("private Image candidateImage;"));
        assertTrue(controller.contains("candidateImage.setValueSource("));
        assertTrue(controller.contains("candidateImage.setSource(ThemeResource.class)"));
    }

    private String readProjectFile(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
