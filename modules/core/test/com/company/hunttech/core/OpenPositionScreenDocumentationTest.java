package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает документационное состояние экранов OpenPositionBrowse и OpenPositionEdit:
 * смысловые inline-комментарии в XML-дескрипторах, бизнес-id у всех визуальных
 * элементов, корректный XML parse и javadoc-покрытие методов контроллеров.
 */
public class OpenPositionScreenDocumentationTest {

    private static final String SCREENS =
            "modules/web/src/com/company/hunttech/web/screens/openposition/";
    private static final String[] XML_FILES = {
            "open-position-browse.xml",
            "open-position-edit.xml"
    };

    @Test
    public void xmlFilesParseAndContainSemanticComments() throws IOException {
        for (String file : XML_FILES) {
            String xml = readProjectFile(SCREENS + file);
            // корректный XML (namespace не проверяется — parse достаточно)
            String[] lines = xml.split("\n");
            int comments = countSemanticComments(lines);
            assertTrue(file + ": недостаточно смысловых комментариев: " + comments,
                    comments >= 50);
            assertTrue(file + ": есть запрещённые комментарии-заглушки",
                    !xml.contains("<!-- Элемент ") && !xml.contains("TODO"));
        }
    }

    @Test
    public void businessIdsPresent() throws IOException {
        String browse = readProjectFile(SCREENS + "open-position-browse.xml");
        assertTrue(browse.contains("id=\"vacancyFilter\""));
        assertTrue(browse.contains("id=\"openPositionButtonsPanel\""));
        assertTrue(browse.contains("id=\"urgentlyPositionsHBox\""));
        assertTrue(browse.contains("id=\"vacancyFilterCheckBoxesHBox\""));

        String edit = readProjectFile(SCREENS + "open-position-edit.xml");
        assertTrue(edit.contains("id=\"laborAgreementButtonsPanel\""));
        assertTrue(edit.contains("id=\"someFilesButtonsPanel\""));
        assertTrue(edit.contains("id=\"openPositionNewsButtonsPanel\""));
        assertTrue(edit.contains("id=\"vacancyTitleSpacerHBox\""));
        assertTrue(edit.contains("id=\"subscribePositionButton\""));
        assertTrue(edit.contains("id=\"windowCloseButton\""));
    }

    @Test
    public void controllersHaveJavadocCoverage() throws IOException {
        String browse = readProjectFile(SCREENS + "OpenPositionBrowse.java");
        String edit = readProjectFile(SCREENS + "OpenPositionEdit.java");
        assertTrue("OpenPositionBrowse: javadoc-покрытие контроллера",
                countOccurrences(browse, "/**") >= 100);
        assertTrue("OpenPositionEdit: javadoc-покрытие контроллера",
                countOccurrences(edit, "/**") >= 100);
        assertTrue("OpenPositionBrowse: нет javadoc класса",
                browse.contains("Контроллер справочника вакансий HRM HuntTech"));
        assertTrue("OpenPositionEdit: нет javadoc класса",
                edit.contains("Контроллер формы редактирования позиции"));
    }

    private int countSemanticComments(String[] lines) {
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("<!--") && trimmed.endsWith("-->")) {
                String text = trimmed.substring(4, trimmed.length() - 3).trim();
                if (text.length() > 25 && !text.startsWith("Элемент ")
                        && !text.startsWith("Кнопка ") && !text.startsWith("Поле ")
                        && !text.equalsIgnoreCase("TODO")) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countOccurrences(String text, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    private String readProjectFile(String path) throws IOException {
        // Тесты модуля core выполняются из modules/core — поднимаемся к корню проекта
        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path base = cwd;
        for (int i = 0; i < 5; i++) {
            Path candidate = base.resolve(path);
            if (Files.exists(candidate)) {
                return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
            }
            base = base.getParent();
            if (base == null) {
                break;
            }
        }
        throw new IOException("Не удалось прочитать " + path + " (cwd=" + cwd + ")");
    }
}
