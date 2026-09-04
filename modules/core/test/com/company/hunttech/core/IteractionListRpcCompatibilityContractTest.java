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
 * Защищает серверный RPC-контракт IteractionListEdit после замены стандартных
 * изображений на OvaFallbackImage. Компонент обязан оставаться наследником
 * стандартного CUBA Image и не регистрировать собственный Vaadin ServerRpc.
 */
public class IteractionListRpcCompatibilityContractTest {

    @Test
    public void ovaFallbackImageUsesStandardCubaImageConnector() throws IOException {
        String webComponent = readProjectFile(
                "modules/web/src/com/hunttech/hrm/web/components/WebOvaFallbackImage.java");
        String componentContract = readProjectFile(
                "modules/gui/src/com/hunttech/hrm/gui/components/OvaFallbackImage.java");
        String ovalContract = readProjectFile(
                "modules/gui/src/com/company/hunttech/gui/components/OvalImage.java");

        assertTrue(webComponent.contains("extends WebImage"));
        assertTrue(componentContract.contains("extends OvalImage, FallbackImage"));
        assertTrue(ovalContract.contains("extends Image"));
        assertFalse(webComponent.contains("registerRpc("));
        assertFalse(webComponent.contains("ServerRpc"));
    }

    @Test
    public void descriptorAndControllerKeepCompatibleImageTypes() throws IOException {
        String descriptor = descriptor();
        String controller = controller();

        assertTrue(descriptor.contains("<ovaFallbackImage id=\"candidateImage\""));
        assertTrue(descriptor.contains("<ovaFallbackImage id=\"projectLogoImage\""));
        assertTrue(controller.contains("private Image candidateImage;"));
        assertTrue(controller.contains("private Image projectLogoImage;"));
        assertTrue(controller.contains("projectLogoImage.setSource(ThemeResource.class)"));
        assertTrue(controller.contains("projectLogoImage.setValueSource("));
    }

    @Test
    public void xmlInvokesExistInControllerAndFrameworkActionsRemainStandard() throws IOException {
        String descriptor = descriptor();
        String controller = controller();

        assertTrue(descriptor.contains("invoke=\"callActionEntity\""));
        assertTrue(controller.contains("void callActionEntity("));
        assertTrue(descriptor.contains("invoke=\"onButtonSubscribeClick\""));
        assertTrue(controller.contains("void onButtonSubscribeClick("));
        assertTrue(descriptor.contains("action=\"windowCommitAndClose\""));
        assertTrue(descriptor.contains("action=\"windowClose\""));
    }

    private String descriptor() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
    }

    private String controller() throws IOException {
        return readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java");
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
