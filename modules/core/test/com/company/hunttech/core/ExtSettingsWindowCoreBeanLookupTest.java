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
    public void middlewareServicesAreResolvedByCubaServiceNameBeforeScreenInitialization() throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindow.java");

        /*
         * Core-реализации находятся в отдельном middleware webapp. Legacy web-контроллер
         * должен получать именованные CUBA service proxy, а не искать core-бин по Java-типу.
         */
        assertFalse(controller.contains("@Inject private ImageProcessingService imageProcessingService;"));
        assertFalse(controller.contains("@Inject private UserAiContextService userAiContextService;"));
        assertTrue(controller.contains("private ImageProcessingService imageProcessingService;"));
        assertTrue(controller.contains("private UserAiContextService userAiContextService;"));
        assertFalse(controller.contains("AppBeans.get(ImageProcessingService.class)"));
        assertFalse(controller.contains("AppBeans.get(UserAiContextService.class)"));
        assertTrue(controller.contains(
                "imageProcessingService = (ImageProcessingService) AppBeans.get(ImageProcessingService.NAME);"));
        assertTrue(controller.contains(
                "userAiContextService = (UserAiContextService) AppBeans.get(UserAiContextService.NAME);"));
        assertTrue(controller.contains("import com.haulmont.cuba.core.global.*;")
                || controller.contains("import com.haulmont.cuba.core.global.AppBeans;"));

        int imageServiceLookup = controller.indexOf(
                "imageProcessingService = (ImageProcessingService) AppBeans.get(ImageProcessingService.NAME);");
        int contextServiceLookup = controller.indexOf(
                "userAiContextService = (UserAiContextService) AppBeans.get(UserAiContextService.NAME);");
        int firstDataLoad = controller.indexOf("currentUser = (ExtUser) userSessionSource");

        assertTrue(imageServiceLookup >= 0 && imageServiceLookup < firstDataLoad);
        assertTrue(contextServiceLookup >= 0 && contextServiceLookup < firstDataLoad);
    }

    @Test
    public void imageProcessingServiceKeepsRemoteSerializableContract() throws IOException {
        String service = readProjectFile(
                "modules/global/src/com/company/hunttech/app/ImageProcessingService.java");
        String bean = readProjectFile(
                "modules/core/src/com/company/hunttech/app/ImageProcessingServiceBean.java");
        String result = readProjectFile(
                "modules/global/src/com/company/hunttech/app/ProcessedImage.java");

        assertTrue(service.contains("String NAME = \"hunttech_ImageProcessingService\";"));
        assertTrue(bean.contains("@Service(ImageProcessingService.NAME)"));
        assertTrue(result.contains("implements Serializable"));
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
