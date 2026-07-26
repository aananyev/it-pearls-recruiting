package com.company.hunttech.core;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает загрузку фотографии CandidateCV в редакторе от регрессии fetch plan,
 * которая приводит к обращению detached-сущности к ValueHolder без persistence session.
 */
public class CandidateCVEditPhotoViewContractTest {

    private static final String SCREEN_XML =
            "modules/web/src/com/company/hunttech/web/screens/candidatecv/candidate-cv-edit.xml";

    @Test
    public void candidateCvRuntimeViewLoadsDirectPhotoAttribute() throws Exception {
        Document document = parseProjectXml(SCREEN_XML);
        Element candidateContainer = findElementById(document, "instance", "candidateCVDc");
        assertNotNull("Не найден контейнер candidateCVDc", candidateContainer);

        Element runtimeView = findDirectChild(candidateContainer, "view");
        assertNotNull("Не найден runtime-view контейнера candidateCVDc", runtimeView);
        assertEquals("candidateCV-view", runtimeView.getAttribute("extends"));

        /*
         * OvaFallbackImage получает CandidateCV.fileImageFace через data binding, поэтому
         * вложенного candidate.fileImageFace недостаточно: верхнеуровневый атрибут обязан
         * быть прямым property runtime-view до отделения сущности от persistence session.
         */
        Element directPhotoProperty = findDirectProperty(runtimeView, "fileImageFace");
        assertNotNull("CandidateCV.fileImageFace отсутствует в runtime-view редактора",
                directPhotoProperty);
        assertEquals("_minimal", directPhotoProperty.getAttribute("view"));
    }

    @Test
    public void photoComponentsKeepCandidateCvFileImageFaceBinding() throws Exception {
        Document document = parseProjectXml(SCREEN_XML);

        Element candidatePic = findElementById(document, "ovaFallbackImage", "candidatePic");
        Element photoUpload = findElementById(document, "upload", "fileImageFaceUpload");

        assertNotNull("Не найден компонент candidatePic", candidatePic);
        assertNotNull("Не найден компонент fileImageFaceUpload", photoUpload);

        // Оба компонента работают с тем же верхнеуровневым атрибутом, который проверяется в runtime-view.
        assertEquals("candidateCVDc", candidatePic.getAttribute("dataContainer"));
        assertEquals("fileImageFace", candidatePic.getAttribute("property"));
        assertEquals("176px", candidatePic.getAttribute("ovalWidth"));
        assertEquals("176px", candidatePic.getAttribute("ovalHeight"));
        assertEquals("icons/no-programmer.jpeg", candidatePic.getAttribute("fallbackThemePath"));
        assertEquals("candidateCVDc", photoUpload.getAttribute("dataContainer"));
        assertEquals("fileImageFace", photoUpload.getAttribute("property"));
    }

    private Document parseProjectXml(String relativePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(resolveProjectFile(relativePath).toFile());
    }

    private Element findElementById(Document document, String localName, String id) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            if (id.equals(element.getAttribute("id"))) {
                return element;
            }
        }
        return null;
    }

    private Element findDirectChild(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && localName.equals(child.getLocalName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private Element findDirectProperty(Element view, String propertyName) {
        NodeList children = view.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element
                    && "property".equals(child.getLocalName())
                    && propertyName.equals(((Element) child).getAttribute("name"))) {
                return (Element) child;
            }
        }
        return null;
    }

    private Path resolveProjectFile(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        Path file = root.resolve(relativePath);
        assertTrue("Не найден файл: " + relativePath, Files.exists(file));
        return file;
    }
}
