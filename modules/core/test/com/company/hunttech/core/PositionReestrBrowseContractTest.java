package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Контрактный тест для экрана PositionReestrBrowse (hunttech_PositionReestr.browse).
 */
public class PositionReestrBrowseContractTest {

    private static final String DESCRIPTOR =
            "modules/web/src/com/company/hunttech/web/screens/position/position-reestr-browse.xml";
    private static final String CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/position/PositionReestrBrowse.java";
    private static final String MENU =
            "modules/web/src/com/company/hunttech/web-menu.xml";

    @Test
    public void descriptorContainsSplitViewElements() throws IOException {
        String xml = readProjectFile(DESCRIPTOR);

        assertTrue(xml.contains("id=\"detailPane\""));
        assertTrue(xml.contains("width=\"312px\""));
        assertTrue(xml.contains("stylename=\"job-candidate-sidebar edit-sidebar\""));
        assertTrue(xml.contains("id=\"sidebarScrollBox\""));
        assertTrue(xml.contains("<ovaFallbackImage id=\"logoPic\""));
        assertTrue(xml.contains("width=\"120px\""));
        assertTrue(xml.contains("height=\"120px\""));

        assertTrue(xml.contains("id=\"workspaceBox\""));
        assertTrue(xml.contains("stylename=\"edit-workspace candidate-reestr-workspace\""));
        assertTrue(xml.contains("id=\"tableFilterBar\""));
        assertTrue(xml.contains("id=\"filter\""));
        assertTrue(xml.contains("id=\"tableCard\""));
        assertTrue(xml.contains("<groupTable id=\"positionsTable\""));
        assertTrue(xml.contains("<rowsCount/>"));
    }

    @Test
    public void menuContainsPositionReestr() throws IOException {
        String menu = readProjectFile(MENU);
        assertTrue(menu.contains("screen=\"hunttech_PositionReestr.browse\""));
    }

    private static String readProjectFile(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8);
    }

    private static Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return root;
    }
}
