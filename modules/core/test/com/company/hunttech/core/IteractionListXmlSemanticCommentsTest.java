package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает обязательную living-документацию XML-дескриптора IteractionListEdit:
 * перед каждым открывающим элементом должен находиться отдельный смысловой комментарий.
 */
public class IteractionListXmlSemanticCommentsTest {

    @Test
    public void everyOpeningElementHasSemanticComment() throws IOException {
        List<String> lines = Files.readAllLines(
                projectRoot().resolve(
                        "modules/web/src/com/company/hunttech/web/screens/iteractionlist/"
                                + "iteraction-list-edit.xml"),
                StandardCharsets.UTF_8);

        List<String> violations = new ArrayList<>();
        boolean insideCdata = false;

        for (int index = 0; index < lines.size(); index++) {
            String trimmed = lines.get(index).trim();

            if (trimmed.contains("<![CDATA[")) {
                insideCdata = true;
            }
            if (insideCdata) {
                if (trimmed.contains("]]>") ) {
                    insideCdata = false;
                }
                continue;
            }

            if (!isOpeningElement(trimmed)) {
                continue;
            }

            int previousIndex = previousNonBlankLine(lines, index - 1);
            if (previousIndex < 0) {
                violations.add("Строка " + (index + 1) + ": отсутствует комментарий перед " + trimmed);
                continue;
            }

            String comment = lines.get(previousIndex).trim();
            if (!isSemanticComment(comment)) {
                violations.add(
                        "Строка " + (index + 1)
                                + ": перед " + trimmed
                                + " найден не смысловой комментарий: " + comment);
            }
        }

        assertTrue(
                "Каждый XML-элемент должен иметь отдельный смысловой комментарий:\n"
                        + String.join("\n", violations),
                violations.isEmpty());
    }

    @Test
    public void commentsExplainPurposeInsteadOfRepeatingTagName() throws IOException {
        String descriptor = readDescriptor();

        assertTrue(descriptor.contains(
                "Корневой экран объявляет editor взаимодействия"));
        assertTrue(descriptor.contains(
                "Контейнер редактируемого взаимодействия хранит текущий экземпляр"));
        assertTrue(descriptor.contains(
                "Sidebar удерживает контекст кандидата, вакансии, навигацию"));
        assertTrue(descriptor.contains(
                "LookupPickerField выбирает вакансию из отфильтрованного openPositionDc"));
        assertTrue(descriptor.contains(
                "TextArea сохраняет обязательное бизнес-описание контакта"));

        assertFalse(descriptor.contains("<!-- Элемент vbox"));
        assertFalse(descriptor.contains("<!-- Элемент label"));
        assertFalse(descriptor.contains("<!-- TODO"));
    }

    private boolean isOpeningElement(String line) {
        return line.startsWith("<")
                && !line.startsWith("<?")
                && !line.startsWith("</")
                && !line.startsWith("<!--")
                && !line.startsWith("<!");
    }

    private boolean isSemanticComment(String line) {
        if (!line.startsWith("<!--") || !line.endsWith("-->")) {
            return false;
        }

        String text = line.substring(4, line.length() - 3).trim();
        return text.length() >= 24
                && text.contains(" ")
                && !text.matches("(?i)элемент\\s+[-_a-z0-9:]+\\.?");
    }

    private int previousNonBlankLine(List<String> lines, int index) {
        while (index >= 0) {
            if (!lines.get(index).trim().isEmpty()) {
                return index;
            }
            index--;
        }
        return -1;
    }

    private String readDescriptor() throws IOException {
        return new String(
                Files.readAllBytes(
                        projectRoot().resolve(
                                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/"
                                        + "iteraction-list-edit.xml")),
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
