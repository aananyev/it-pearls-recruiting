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
 * Защищает living-документацию XML-дескриптора IteractionListEdit:
 * каждый открывающий UI/data element должен быть предварён отдельным
 * смысловым комментарием, объясняющим его роль в новой форме.
 */
public class IteractionListXmlSemanticCommentsTest {

    @Test
    public void everyOpeningElementHasMeaningfulCommentImmediatelyBeforeIt() throws IOException {
        List<String> lines = normalizedLines(readDescriptor());

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!isOpeningElement(line)) {
                continue;
            }

            int previous = previousNonBlankLine(lines, i - 1);
            assertTrue("Перед XML-элементом отсутствует комментарий: " + line,
                    previous >= 0);
            String comment = lines.get(previous).trim();
            assertTrue("Комментарий недостаточно смысловой перед: " + line + " -> " + comment,
                    isSemanticComment(comment));
        }
    }

    @Test
    public void commentsDescribeBusinessAndLayoutPurposeInsteadOfRepeatingTagNames()
            throws IOException {
        String descriptor = readDescriptor();

        assertTrue(descriptor.contains(
                "Data layer сохраняет исходные containers, views, loaders и JPQL-контракты"));
        assertTrue(descriptor.contains(
                "Sidebar сохраняет контекст кандидата/вакансии"));
        assertTrue(descriptor.contains(
                "Quick actions остаются отдельной карточкой"));
        assertTrue(descriptor.contains(
                "DateTime add-field включается для addType=1"));
        assertTrue(descriptor.contains(
                "TextArea сохраняет comment"));

        assertFalse(descriptor.contains("<!-- Элемент vbox"));
        assertFalse(descriptor.contains("<!-- Элемент label"));
        assertFalse(descriptor.contains("<!-- TODO"));
    }

    private List<String> normalizedLines(String descriptor) {
        String[] raw = descriptor.replace("\r\n", "\n").split("\n");
        List<String> lines = new ArrayList<>();
        for (String line : raw) {
            lines.add(line);
        }
        return lines;
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
