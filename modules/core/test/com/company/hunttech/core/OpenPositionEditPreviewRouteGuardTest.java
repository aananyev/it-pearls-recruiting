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
 * Защищает preview от повторного появления ошибки URL-навигации, при которой
 * detached OpenPosition обращался к незагруженной lazy-связи positionType.
 */
public class OpenPositionEditPreviewRouteGuardTest {

    private static final String PREVIEW_CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEditPreview.java";
    private static final String LEGACY_CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEdit.java";

    @Test
    public void routeGuardLoadsPositionTypeBeforeInheritedLifecycle() throws IOException {
        String previewController = readProjectFile(PREVIEW_CONTROLLER);
        String legacyController = readProjectFile(LEGACY_CONTROLLER);

        assertTrue(previewController.contains(
                "import com.haulmont.cuba.gui.screen.Screen.BeforeShowEvent;"));
        assertTrue(previewController.contains(
                "public void onBeforeShow(BeforeShowEvent event)"));
        assertTrue(previewController.contains(
                "PersistenceHelper.isLoaded(editedPosition, \"positionType\")"));
        assertTrue(previewController.contains("dataManager.load(OpenPosition.class)"));
        assertTrue(previewController.contains(
                ".add(\"positionType\", positionType -> positionType"));
        // сеттер на detached entity с lazy positionType триггерит старый valueholder
        // (ValidationException null Session) — заменяем item контейнера целиком
        assertTrue(previewController.contains(
                "getEditedEntityContainer().setItem(reloadedPosition)"));
        assertFalse(previewController.contains(
                "setPositionType(reloadedPosition.getPositionType()"));

        assertOrdered(previewController,
                "ensureRoutePositionTypeLoaded();",
                "super.onBeforeShow(event);");
        assertFalse(legacyController.contains("ensureRoutePositionTypeLoaded"));
    }

    private void assertOrdered(String text, String firstMarker, String secondMarker) {
        int first = text.indexOf(firstMarker);
        int second = text.indexOf(secondMarker);
        assertTrue("Не найден первый обязательный маркер", first >= 0);
        assertTrue("Не найден второй обязательный маркер", second >= 0);
        assertTrue("Догрузка positionType должна выполняться до legacy lifecycle", first < second);
    }

    private String readProjectFile(String relativePath) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relativePath)),
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
