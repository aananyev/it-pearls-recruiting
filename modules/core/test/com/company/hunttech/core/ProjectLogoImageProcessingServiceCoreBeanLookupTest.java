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
 * Контракт web/core интеграции сервиса обработки логотипа проекта.
 *
 * <p>Защищает от регрессии, воспроизводившей NoSuchBeanDefinitionException при загрузке
 * логотипа в ProjectEdit: core-бин недоступен из web-контекста без записи в
 * WebRemoteProxyBeanCreator, а class-based lookup запрещён (см. ExtSettingsWindowCoreBeanLookupTest).</p>
 */
public class ProjectLogoImageProcessingServiceCoreBeanLookupTest {

    @Test
    public void webContextRegistersProjectLogoServiceProxy() throws IOException {
        String webSpring = readProjectFile("modules/web/src/com/company/hunttech/web-spring.xml");

        assertTrue(webSpring.contains(
                "key=\"hunttech_ProjectLogoImageProcessingService\"\n"
                        + "                       value=\"com.company.hunttech.app.ProjectLogoImageProcessingService\""));
    }

    @Test
    public void projectLogoServiceKeepsRemoteSerializableContract() throws IOException {
        String service = readProjectFile(
                "modules/global/src/com/company/hunttech/app/ProjectLogoImageProcessingService.java");
        String bean = readProjectFile(
                "modules/core/src/com/company/hunttech/app/ProjectLogoImageProcessingServiceBean.java");
        String result = readProjectFile(
                "modules/global/src/com/company/hunttech/app/ProcessedImage.java");

        assertTrue(service.contains("String NAME = \"hunttech_ProjectLogoImageProcessingService\";"));
        assertTrue(bean.contains("@Service(ProjectLogoImageProcessingService.NAME)"));
        assertTrue(result.contains("implements Serializable"));
    }

    @Test
    public void uploadComponentResolvesServiceByNameNotByClass() throws IOException {
        String component = readProjectFile(
                "modules/web/src/com/company/hunttech/web/gui/components/WebProjectLogoFileUploadField.java");

        assertTrue(component.contains("beanLocator.get(ProjectLogoImageProcessingService.NAME)"));
        assertFalse(component.contains("AppBeans.get(ProjectLogoImageProcessingService.class)"));
        assertFalse(component.contains("@Inject private ProjectLogoImageProcessingService"));
    }

    @Test
    public void logoProcessingUsesAiFunctionContractWithDeterministicFallback() throws IOException {
        String bean = readProjectFile(
                "modules/core/src/com/company/hunttech/app/ProjectLogoImageProcessingServiceBean.java");

        assertTrue(bean.contains("FUNCTION_PROJECT_LOGO_IMAGE_GENERATE = \"PROJECT_LOGO_IMAGE_GENERATE\""));
        assertTrue(bean.contains("aiExecutionService.executeImage("));
        assertTrue(bean.contains("tryAiBackgroundRemoval"));
        assertTrue(bean.contains("используется классический конвейер"));
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
