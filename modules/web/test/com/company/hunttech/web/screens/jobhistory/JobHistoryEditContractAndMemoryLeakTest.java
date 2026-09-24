package com.company.hunttech.web.screens.jobhistory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Автотест экрана JobHistoryEdit (BL-2026-OOM-JH):
 * Защита от OutOfMemoryError: Java heap space.
 * Проверяет отсутствие unbounded collection loaders (candidatesDc, currentCompaniesDc),
 * загружавших десятки тысяч записей в память, и подтверждает использование SuggestionPickerField.
 */
class JobHistoryEditContractAndMemoryLeakTest {

    private File resolveFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        if (path.startsWith("modules/web/")) {
            File subFile = new File(path.substring("modules/web/".length()));
            if (subFile.exists()) {
                return subFile;
            }
        }
        Path root = Paths.get("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        if (root != null) {
            File resolved = root.resolve(path).toFile();
            if (resolved.exists()) {
                return resolved;
            }
        }
        return file;
    }

    private Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    @Test
    void testXmlDescriptorPreventsOutOfMemory() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/jobhistory/job-history-edit.xml");
        assertTrue(xmlFile.exists(), "Файл job-history-edit.xml должен существовать");

        Document doc = parseXml(xmlFile);
        Element root = doc.getDocumentElement();

        // 1. Проверяем, что в блоке <data> нет опасных unbounded-коллекций candidatesDc и currentCompaniesDc
        NodeList collections = root.getElementsByTagName("collection");
        for (int i = 0; i < collections.getLength(); i++) {
            Element col = (Element) collections.item(i);
            String id = col.getAttribute("id");
            assertNotEquals("candidatesDc", id,
                    "КРИТИЧНО: candidatesDc с полным сканированием базы кандидатов вызывает OutOfMemoryError!");
            assertNotEquals("currentCompaniesDc", id,
                    "КРИТИЧНО: currentCompaniesDc с полным сканированием компаний вызывает OutOfMemoryError!");
        }

        // 2. Проверяем, что нет lookupPickerField с опциями candidatesDc / currentCompaniesDc
        NodeList lookupPickerFields = root.getElementsByTagName("lookupPickerField");
        for (int i = 0; i < lookupPickerFields.getLength(); i++) {
            Element lpf = (Element) lookupPickerFields.item(i);
            String optionsContainer = lpf.getAttribute("optionsContainer");
            assertNotEquals("candidatesDc", optionsContainer, "Недопустимо использовать optionsContainer='candidatesDc'");
            assertNotEquals("currentCompaniesDc", optionsContainer, "Недопустимо использовать optionsContainer='currentCompaniesDc'");
        }

        // 3. Проверяем, что ключевые сущности связаны через suggestionPickerField с защитой лимитами
        NodeList suggestionFields = root.getElementsByTagName("suggestionPickerField");
        assertTrue(suggestionFields.getLength() >= 2,
                "Должны использоваться suggestionPickerField для компании и/или кандидата");

        boolean candidateFound = false;
        boolean companyFound = false;
        boolean positionFound = false;

        for (int i = 0; i < suggestionFields.getLength(); i++) {
            Element spf = (Element) suggestionFields.item(i);
            String id = spf.getAttribute("id");
            if ("candidateField".equals(id)) {
                candidateFound = true;
                int minLen = Integer.parseInt(spf.getAttribute("minSearchStringLength"));
                assertTrue(minLen >= 2, "candidateField должен иметь minSearchStringLength >= 2");
            } else if ("currentCompanyField".equals(id)) {
                companyFound = true;
                int minLen = Integer.parseInt(spf.getAttribute("minSearchStringLength"));
                assertTrue(minLen >= 2, "currentCompanyField должен иметь minSearchStringLength >= 2");
            } else if ("currentPositionField".equals(id)) {
                positionFound = true;
            }
        }

        assertTrue(candidateFound, "candidateField должен быть объявлен как suggestionPickerField");
        assertTrue(companyFound, "currentCompanyField должен быть объявлен как suggestionPickerField");
        assertTrue(positionFound, "currentPositionField должен быть объявлен как suggestionPickerField");

        // 4. Проверяем наличие всех необходимых полей сущности JobHistory
        assertNotNull(findElementById(root, "startDateField"), "startDateField должен присутствовать");
        assertNotNull(findElementById(root, "endDateField"), "endDateField должен присутствовать");
        assertNotNull(findElementById(root, "dutiesField"), "dutiesField должен присутствовать");
        assertNotNull(findElementById(root, "rawCompanyNameField"), "rawCompanyNameField должен присутствовать");
        assertNotNull(findElementById(root, "rawPositionNameField"), "rawPositionNameField должен присутствовать");
    }

    @Test
    void testControllerClassHasLoadDataBeforeShow() throws Exception {
        File javaFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/jobhistory/JobHistoryEdit.java");
        assertTrue(javaFile.exists(), "Файл JobHistoryEdit.java должен существовать");

        String content = new String(Files.readAllBytes(javaFile.toPath()));
        assertTrue(content.contains("@LoadDataBeforeShow"),
                "Контроллер JobHistoryEdit ОБЯЗАН содержать @LoadDataBeforeShow, иначе редактируемая сущность не загружается и форма открывается пустой!");
        assertTrue(content.contains("logMemState"),
                "Контроллер JobHistoryEdit должен содержать логирование состояния памяти JVM");
    }

    private Element findElementById(Element parent, String id) {
        if (id.equals(parent.getAttribute("id"))) {
            return parent;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element found = findElementById((Element) children.item(i), id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
