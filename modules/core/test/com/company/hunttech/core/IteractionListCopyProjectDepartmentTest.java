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
 * Защищает сценарий «Копировать» от передачи в IteractionListEdit
 * сокращённого detached-графа вакансии без projectDepartment.
 */
public class IteractionListCopyProjectDepartmentTest {

    @Test
    public void copyReloadsVacancyWithEditorViewBeforeOpeningNewInteraction() throws IOException {
        String browseController = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListBrowse.java");
        String views = readProjectFile("modules/global/src/com/company/hunttech/views.xml");

        assertTrue(browseController.contains(
                "OPEN_POSITION_COPY_VIEW = \"openPosition-iteraction-list-picker-view\""));
        assertTrue(browseController.contains(
                "data.setVacancy(loadVacancyForCopy(selectedInteraction.getVacancy()))"));
        assertTrue(browseController.contains(
                "dataManager.reload(vacancy, OPEN_POSITION_COPY_VIEW)"));
        assertFalse(browseController.contains(
                "data.setVacancy(iteractionListsTable.getSingleSelected().getVacancy())"));

        String copyView = extractView(views, "openPosition-iteraction-list-picker-view");
        assertTrue(copyView.contains("<property name=\"projectName\""));
        assertTrue(copyView.contains("<property name=\"projectDepartment\""));
        assertTrue(copyView.contains("<property name=\"departamentRuName\"/>"));
        assertTrue(copyView.contains("<property name=\"companyName\""));
        assertTrue(copyView.contains("<property name=\"companyShortName\"/>"));
    }

    private String extractView(String views, String viewName) {
        String marker = "name=\"" + viewName + "\"";
        int markerIndex = views.indexOf(marker);
        assertTrue("Не найден обязательный view: " + viewName, markerIndex >= 0);

        int viewStart = views.lastIndexOf("<view", markerIndex);
        int viewEnd = views.indexOf("</view>", markerIndex);
        assertTrue("Не удалось определить границы view: " + viewName,
                viewStart >= 0 && viewEnd > markerIndex);
        return views.substring(viewStart, viewEnd + "</view>".length());
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
