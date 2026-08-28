package com.company.hunttech.core;

import org.junit.Test;

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
 * Презентационный контракт для экрана PositionEdit (hunttech_Position.edit).
 */
public class PositionEditLayoutContractTest {

    private static final String DESCRIPTOR =
            "modules/web/src/com/company/hunttech/web/screens/position/position-edit.xml";
    private static final String CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/position/PositionEdit.java";
    private static final String LOCAL_SCSS =
            "modules/web/themes/hover/com.company.hunttech/position-editor.scss";
    private static final List<String> THEMES = Arrays.asList(
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark"
    );

    @Test
    public void descriptorFollowsStandardEditComposition() throws IOException {
        String xml = readProjectFile(DESCRIPTOR);

        assertTrue(xml.contains("stylename=\"position-editor\""));
        assertTrue(xml.contains("stylename=\"edit-screen-layout\""));
        assertTrue(xml.contains("stylename=\"edit-sidebar\""));
        assertTrue(xml.contains("width=\"270px\""));
        assertTrue(xml.contains("stylename=\"edit-sidebar-visual\""));
        assertTrue(xml.contains("stylename=\"edit-sidebar-identity\""));
        assertTrue(xml.contains("stylename=\"edit-sidebar-title\""));
        assertTrue(xml.contains("stylename=\"label-navigation\""));
        assertTrue(xml.contains("stylename=\"edit-sidebar-summary\""));
        assertTrue(xml.contains("stylename=\"edit-sidebar-spacer\""));
        assertTrue(xml.contains("stylename=\"edit-sidebar-hint\""));

        assertTrue(xml.contains("stylename=\"edit-workspace\""));
        assertTrue(xml.contains("stylename=\"edit-toolbar\""));
        assertTrue(xml.contains("stylename=\"edit-workspace-scroll\""));
        assertTrue(xml.contains("stylename=\"edit-workspace-content\""));
        assertTrue(xml.contains("stylename=\"edit-footer-actions\""));

        // Удалена подпись типа записи из identity
        assertFalse(xml.contains("stylename=\"edit-sidebar-subtitle\" width=\"100%\""));
    }

    @Test
    public void scssDefinesDarkSidebarAndIsSynchronized() throws IOException {
        String canon = readProjectFile(LOCAL_SCSS);
        assertTrue(canon.contains("#172638"));
        assertTrue(canon.contains("linear-gradient(180deg, #172638 0%, #132130 58%, #0f1b28 100%)"));
        assertTrue(canon.contains(".v-richtextarea"));

        for (String theme : THEMES) {
            String scss = readProjectFile("modules/web/themes/" + theme + "/com.company.hunttech/position-editor.scss");
            assertEquals("SCSS file for theme " + theme + " must match hover", canon, scss);
        }
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
