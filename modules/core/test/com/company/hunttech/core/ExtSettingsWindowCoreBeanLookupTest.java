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

public class ExtSettingsWindowCoreBeanLookupTest {

    @Test
    public void coreServicesAreResolvedThroughAppBeansBeforeScreenInitialization() throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindow.java");

        /*
         * Legacy web-контроллер не должен полагаться на @Inject для core-Spring-бинов:
         * сервисы разрешаются из общего контекста CUBA до первого обращения к данным экрана.
         */
        assertFalse(controller.contains("@Inject private ImageProcessingService imageProcessingService;"));
        assertFalse(controller.contains("@Inject private UserAiContextService userAiContextService;"));
        assertTrue(controller.contains("private ImageProcessingService imageProcessingService;"));
        assertTrue(controller.contains("private UserAiContextService userAiContextService;"));
        assertTrue(controller.contains("imageProcessingService = AppBeans.get(ImageProcessingService.class);"));
        assertTrue(controller.contains("userAiContextService = AppBeans.get(UserAiContextService.class);"));
        assertTrue(controller.contains("import com.haulmont.cuba.core.global.*;")
                || controller.contains("import com.haulmont.cuba.core.global.AppBeans;"));

        int imageServiceLookup = controller.indexOf(
                "imageProcessingService = AppBeans.get(ImageProcessingService.class);");
        int contextServiceLookup = controller.indexOf(
                "userAiContextService = AppBeans.get(UserAiContextService.class);");
        int firstDataLoad = controller.indexOf("currentUser = (ExtUser) userSessionSource");

        assertTrue(imageServiceLookup >= 0 && imageServiceLookup < firstDataLoad);
        assertTrue(contextServiceLookup >= 0 && contextServiceLookup < firstDataLoad);
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
