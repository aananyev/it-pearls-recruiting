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
 * Защищает удаление блока label-навигации за ненадобностью после
 * объединения всех полей в единый информационный блок.
 */
public class IteractionListAccordionNavigationTest {

    @Test
    public void activeControllerDoesNotContainLabelNavigation() throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java");

        assertFalse(controller.contains("iteractionListNavigation"));
        assertFalse(controller.contains("participantsAccordionNav"));
        assertFalse(controller.contains("interactionAccordionNav"));
        assertFalse(controller.contains("resultAccordionNav"));
        assertFalse(controller.contains("commentAccordionNav"));
        assertFalse(controller.contains("selectSection("));
    }

    @Test
    public void descriptorDoesNotContainLabelNavigationInSidebar() throws IOException {
        String descriptor = descriptor();

        assertFalse(descriptor.contains("id=\"iteractionListNavigation\""));
        assertFalse(descriptor.contains("id=\"iteractionListNavigationTitle\""));
        assertFalse(descriptor.contains("msgAccordionNavigation"));
    }

    private String descriptor() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
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
