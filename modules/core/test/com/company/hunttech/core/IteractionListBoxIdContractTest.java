package com.company.hunttech.core;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает читаемость XML-компоновки IteractionListEdit: каждый Box-компонент
 * обязан иметь уникальный ID, который отражает его назначение на экране.
 */
public class IteractionListBoxIdContractTest {

    private static final Set<String> BOX_TAGS = new HashSet<>(Arrays.asList(
            "vbox", "hbox", "scrollBox", "buttonsPanel"
    ));

    private static final Set<String> REQUIRED_IDS = new HashSet<>(Arrays.asList(
            "iteractionProfileSummaryBox",
            "vacancyStatusValueBox",
            "vacancyPriorityValueBox",
            "vacancyCompanyDepartmentBox",
            "vacancyProjectBox",
            "outstaffingCostContentBox",
            "outstaffingCostValueBox",
            "vacancyRatingContextBox",
            "iteractionListToolbarBox",
            "iteractionListSectionsBox",
            "participantsSectionHeaderBox",
            "interactionSectionHeaderBox",
            "interactionSectionBodyBox",
            "dynamicActionFieldsBox",
            "resultSectionHeaderBox",
            "commentSectionHeaderBox",
            "commentSectionBodyBox"
    ));

    @Test
    public void everyBoxHasUniqueSemanticId() throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(descriptor().toFile());

        Set<String> ids = new HashSet<>();
        Set<String> foundRequiredIds = new HashSet<>();
        NodeList nodes = document.getElementsByTagName("*");

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }

            Element element = (Element) node;
            String tag = element.getTagName();
            if (!BOX_TAGS.contains(tag)) {
                continue;
            }

            String id = element.getAttribute("id");
            assertFalse("Box-компонент <" + tag + "> не имеет id", id.trim().isEmpty());
            assertTrue("Обнаружен повторяющийся Box id: " + id, ids.add(id));
            assertSemantic(id, tag);

            if (REQUIRED_IDS.contains(id)) {
                foundRequiredIds.add(id);
            }
        }

        assertTrue(
                "Не найдены обязательные смысловые Box ID: " + difference(REQUIRED_IDS, foundRequiredIds),
                foundRequiredIds.containsAll(REQUIRED_IDS));
        assertFalse("Устаревший общий id labelHBox не должен использоваться",
                ids.contains("labelHBox"));
    }

    private void assertSemantic(String id, String tag) {
        String normalized = id.toLowerCase();
        assertTrue("Слишком короткий id для <" + tag + ">: " + id, id.length() >= 8);
        assertFalse("Технический id не отражает назначение: " + id,
                normalized.matches("(vbox|hbox|box|layout|container|panel|row|column)\\d*"));
        assertFalse("ID не должен начинаться с generic-типа компонента: " + id,
                normalized.startsWith("vbox") || normalized.startsWith("hbox"));
    }

    private Set<String> difference(Set<String> expected, Set<String> actual) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }

    private Path descriptor() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return root.resolve(
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
    }
}
