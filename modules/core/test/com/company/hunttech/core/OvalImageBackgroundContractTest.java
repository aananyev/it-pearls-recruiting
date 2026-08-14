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
 * Защищает контракт атрибута {@code ovalBackground} (фон-подложка под прозрачные
 * изображения) кастомных компонентов OvalImage / OvaFallbackImage HRM HuntTech.
 *
 * <p>Паттерн — файловый контрактный тест, как {@code LeftSidebarAvatarComponentTest}:
 * проверяет API в исходниках, чтобы регресс (удаление метода, переименование
 * XML-атрибута) ловился без поднятия Vaadin-контейнера.</p>
 */
public class OvalImageBackgroundContractTest {

    @Test
    public void ovalImageInterfaceDeclaresBackgroundApi() throws IOException {
        String gui = readProjectFile("modules/gui/src/com/company/hunttech/gui/components/OvalImage.java");
        assertTrue("Интерфейс OvalImage должен объявлять getOvalBackground()",
                gui.contains("String getOvalBackground();"));
        assertTrue("Интерфейс OvalImage должен объявлять setOvalBackground(String)",
                gui.contains("void setOvalBackground(String background);"));
    }

    @Test
    public void webOvalImageImplementsBackgroundApi() throws IOException {
        String web = readProjectFile("modules/web/src/com/company/hunttech/web/gui/components/WebOvalImage.java");
        assertTrue("WebOvalImage должен реализовывать getOvalBackground()",
                web.contains("public String getOvalBackground()"));
        assertTrue("WebOvalImage должен реализовывать setOvalBackground(String)",
                web.contains("public void setOvalBackground(String background)"));
        assertTrue("WebOvalImage должен применять фон через OvalImageBackgroundSupport",
                web.contains("OvalImageBackgroundSupport.applyBackground"));
    }

    @Test
    public void ovalImageLoaderReadsBackgroundAttribute() throws IOException {
        String loader = readProjectFile(
                "modules/web/src/com/company/hunttech/web/gui/xml/layout/loaders/OvalImageLoader.java");
        assertTrue("OvalImageLoader должен читать XML-атрибут ovalBackground",
                loader.contains("element.attributeValue(\"ovalBackground\")"));
        assertTrue("OvalImageLoader должен вызывать setOvalBackground",
                loader.contains("setOvalBackground(ovalBackground)"));
    }

    @Test
    public void ovaFallbackImageImplementsInheritedBackgroundApi() throws IOException {
        String ova = readProjectFile(
                "modules/web/src/com/hunttech/hrm/web/components/WebOvaFallbackImage.java");
        assertTrue("WebOvaFallbackImage (OvaFallbackImage extends OvalImage) должен реализовывать getOvalBackground()",
                ova.contains("public String getOvalBackground()"));
        assertTrue("WebOvaFallbackImage должен реализовывать setOvalBackground(String)",
                ova.contains("public void setOvalBackground(String background)"));
        assertTrue("WebOvaFallbackImage должен применять фон через OvalImageBackgroundSupport",
                ova.contains("OvalImageBackgroundSupport.applyBackground"));
    }

    @Test
    public void ovaFallbackImageLoaderReadsBackgroundAttribute() throws IOException {
        String loader = readProjectFile(
                "modules/web/src/com/hunttech/hrm/web/loaders/OvaFallbackImageLoader.java");
        assertTrue("OvaFallbackImageLoader должен читать XML-атрибут ovalBackground",
                loader.contains("element.attributeValue(\"ovalBackground\")"));
        assertTrue("OvaFallbackImageLoader должен вызывать setOvalBackground",
                loader.contains("setOvalBackground(ovalBackground)"));
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
