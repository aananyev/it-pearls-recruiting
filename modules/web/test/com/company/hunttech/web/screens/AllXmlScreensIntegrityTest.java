package com.company.hunttech.web.screens;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Автоматизированный этап валидации и анализа всех XML-дескрипторов экранных форм HRM.
 * 
 * Выполняет предстартовую валидацию:
 * 1. Well-formedness и корректность синтаксиса XML парсером W3C DOM.
 * 2. Корректность атрибута expand (запрет списков через запятую, проверка существования дочернего компонента).
 * 3. Целостность ссылок на dataContainer (dataContainer должен быть задекларирован в блоке <data>).
 * 4. Уникальность идентификаторов компонентов внутри экрана.
 */
public class AllXmlScreensIntegrityTest {

    @Test
    @DisplayName("Валидация всех XML экранных форм и фрагментов проекта на корректность синтаксиса, expand и привязок")
    void testAllXmlScreensIntegrity() throws Exception {
        List<File> screenXmlFiles = findScreenXmlFiles();
        assertTrue(screenXmlFiles.size() > 10, "Должно быть найдено достаточное количество XML-экранов (найдено: " + screenXmlFiles.size() + ")");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(false);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        List<String> validationErrors = new ArrayList<>();

        for (File xmlFile : screenXmlFiles) {
            validateScreenXml(xmlFile, dBuilder, validationErrors);
        }

        if (!validationErrors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n================================================================================\n");
            sb.append("ОБНАРУЖЕНЫ ОШИБКИ В XML-ДЕСКРИПТОРАХ ЭКРАНОВ (").append(validationErrors.size()).append("):\n");
            sb.append("================================================================================\n");
            for (String error : validationErrors) {
                sb.append(" ❌ ").append(error).append("\n");
            }
            sb.append("================================================================================\n");
            fail(sb.toString());
        }
    }

    private void validateScreenXml(File xmlFile, DocumentBuilder builder, List<String> errors) {
        Document doc;
        try {
            doc = builder.parse(xmlFile);
        } catch (Exception ex) {
            errors.add(xmlFile.getAbsolutePath() + ": Ошибка синтаксиса XML: " + ex.getMessage());
            return;
        }

        Element root = doc.getDocumentElement();
        if (root == null) {
            return;
        }

        String rootTag = root.getTagName();
        if (!"window".equals(rootTag) && !"fragment".equals(rootTag)) {
            // Не является дескриптором экрана CUBA
            return;
        }

        // Собираем объявленные dataContainer (включая все вложенные collection и instance на любой глубине)
        Set<String> declaredDataContainers = new HashSet<>();
        NodeList dataNodes = doc.getElementsByTagName("data");
        for (int i = 0; i < dataNodes.getLength(); i++) {
            Element dataEl = (Element) dataNodes.item(i);
            NodeList subElements = dataEl.getElementsByTagName("*");
            for (int j = 0; j < subElements.getLength(); j++) {
                Element child = (Element) subElements.item(j);
                String tag = child.getTagName();
                if ("instance".equals(tag) || "collection".equals(tag)
                        || "keyValueCollection".equals(tag) || "keyValueInstance".equals(tag)) {
                    String id = child.getAttribute("id");
                    if (id != null && !id.trim().isEmpty()) {
                        declaredDataContainers.add(id.trim());
                    }
                }
            }
        }

        // Также учитываем legacy dsContext
        NodeList dsNodes = doc.getElementsByTagName("dsContext");
        for (int i = 0; i < dsNodes.getLength(); i++) {
            Element dsEl = (Element) dsNodes.item(i);
            NodeList subElements = dsEl.getElementsByTagName("*");
            for (int j = 0; j < subElements.getLength(); j++) {
                Element child = (Element) subElements.item(j);
                String id = child.getAttribute("id");
                if (id != null && !id.trim().isEmpty()) {
                    declaredDataContainers.add(id.trim());
                }
            }
        }

        // Проверяем все элементы документа
        NodeList allElements = doc.getElementsByTagName("*");
        Set<String> seenIds = new HashSet<>();

        for (int i = 0; i < allElements.getLength(); i++) {
            Element el = (Element) allElements.item(i);

            // 1. Проверка атрибута expand
            if (el.hasAttribute("expand")) {
                String expandVal = el.getAttribute("expand").trim();
                if (expandVal.isEmpty()) {
                    errors.add(xmlFile.getName() + " [<" + el.getTagName() + " id=\"" + el.getAttribute("id") + "\">]: "
                            + "Атрибут expand не должен быть пустым");
                } else if (expandVal.contains(",") || expandVal.contains(";") || expandVal.contains(" ")) {
                    errors.add(xmlFile.getName() + " [<" + el.getTagName() + " id=\"" + el.getAttribute("id") + "\">]: "
                            + "Недопустимый составной expand='" + expandVal + "'. CUBA Platform expand принимает ровно один идентификатор компонента!");
                } else {
                    // Проверяем, что компонент с таким id существует внутри контейнера
                    boolean childFound = hasDescendantWithId(el, expandVal);
                    if (!childFound) {
                        errors.add(xmlFile.getName() + " [<" + el.getTagName() + " id=\"" + el.getAttribute("id") + "\">]: "
                                + "Компонент с id='" + expandVal + "' для expand не найден среди дочерних элементов");
                    }
                }
            }

            // 2. Проверка ссылки на dataContainer
            if (el.hasAttribute("dataContainer")) {
                String containerRef = el.getAttribute("dataContainer").trim();
                if (!containerRef.isEmpty() && !declaredDataContainers.isEmpty() && !declaredDataContainers.contains(containerRef)) {
                    errors.add(xmlFile.getName() + " [<" + el.getTagName() + " id=\"" + el.getAttribute("id") + "\">]: "
                            + "Ссылка на необъявленный dataContainer='" + containerRef + "'. Объявлены: " + declaredDataContainers);
                }
            }

            // 3. Сбор ID для проверки уникальности компонентов верхнего уровня
            if (el.hasAttribute("id")) {
                String compId = el.getAttribute("id").trim();
                if (!compId.isEmpty()) {
                    String tag = el.getTagName();
                    // Исключаем системные действия и общие вложенные теги
                    if (!"action".equals(tag) && !"column".equals(tag) && !"property".equals(tag) && !"param".equals(tag) && !"tab".equals(tag)) {
                        if (seenIds.contains(compId)) {
                            // Дубликат ID
                            errors.add(xmlFile.getName() + ": Обнаружен дубликат идентификатора компонента id='" + compId + "' в теге <" + tag + ">");
                        } else {
                            seenIds.add(compId);
                        }
                    }
                }
            }
        }
    }

    private boolean hasDescendantWithId(Element parent, String targetId) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) children.item(i);
                if (targetId.equals(child.getAttribute("id"))) {
                    return true;
                }
                if (hasDescendantWithId(child, targetId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<File> findScreenXmlFiles() {
        List<File> result = new ArrayList<>();
        String[] searchDirs = {"modules/web/src", "modules/gui/src"};
        for (String dirPath : searchDirs) {
            File dir = new File(dirPath);
            if (!dir.exists() && dirPath.startsWith("modules/")) {
                dir = new File(dirPath.substring("modules/web/".length()));
            }
            if (dir.exists()) {
                try (Stream<Path> stream = Files.walk(dir.toPath())) {
                    List<File> files = stream
                            .filter(p -> p.toString().endsWith(".xml"))
                            .map(Path::toFile)
                            .filter(f -> !f.getName().equals("screens.xml") && !f.getName().equals("web-menu.xml"))
                            .collect(Collectors.toList());
                    result.addAll(files);
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }
}
