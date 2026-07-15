package com.company.hunttech.web.screens.jobcandidate;

import org.junit.Test;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Предотвращает регрессию скалярных запросов в suggestionField/suggestionPickerField.
 * CUBA требует entity-запрос (select e) для корректной работы подсказок.
 * Скалярные запросы (select e.fieldName) ломают выпадающий список и ввод.
 */
public class SuggestionFieldQueryIntegrityTest {

    private static final String XML_PATH = "/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml";

    @Test
    public void allSuggestionFieldQueriesUseEntitySelect() throws Exception {
        Document doc = parseXml();
        NodeList queries = xpath(doc, "//suggestionField/query | //suggestionPickerField/query");
        assertTrue("Должны быть suggestionField/suggestionPickerField с query", queries.getLength() > 0);

        for (int i = 0; i < queries.getLength(); i++) {
            Element queryEl = (Element) queries.item(i);
            String fieldId = parentAttr(queryEl, "id");
            String jpql = normalize(queryEl.getTextContent());

            // JPQL должен начинаться с "select e" (entity-запрос), не с "select e.something"
            assertTrue(
                    String.format("Поле %s: JPQL должен быть entity-запросом (select e ...), получено: %s",
                            fieldId, firstWords(jpql, 5)),
                    jpql.startsWith("select e ") || jpql.startsWith("SELECT E "));
        }
    }

    @Test
    public void noFieldHasEscapeFalse() throws Exception {
        Document doc = parseXml();
        NodeList queries = xpath(doc, "//suggestionField/query | //suggestionPickerField/query");

        for (int i = 0; i < queries.getLength(); i++) {
            Element queryEl = (Element) queries.item(i);
            String escapeAttr = queryEl.getAttribute("escapeValueForLike");
            String fieldId = parentAttr(queryEl, "id");

            assertFalse(
                    String.format("Поле %s: escapeValueForLike=\"false\" недопустим", fieldId),
                    "false".equals(escapeAttr));
        }
    }

    @Test
    public void tabMainHasNoScalarQueries() throws Exception {
        Document doc = parseXml();
        NodeList queries = xpath(doc,
                "//tab[@id='tabMain']//suggestionField/query | //tab[@id='tabMain']//suggestionPickerField/query");

        for (int i = 0; i < queries.getLength(); i++) {
            Element queryEl = (Element) queries.item(i);
            String fieldId = parentAttr(queryEl, "id");
            String jpql = normalize(queryEl.getTextContent());

            assertTrue(
                    String.format("tabMain поле %s: ожидается entity-запрос (select e ...)", fieldId),
                    jpql.startsWith("select e ") || jpql.startsWith("SELECT E "));

            assertTrue(
                    String.format("tabMain поле %s: escapeValueForLike должен быть 'true'", fieldId),
                    "true".equals(queryEl.getAttribute("escapeValueForLike")));
        }
    }

    // ── helpers ──

    /** Схлапывает пробелы и переносы строк в один пробел и обрезает. */
    static String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    /** Первые N слов строки для читаемого assert-сообщения. */
    static String firstWords(String s, int n) {
        String[] words = s.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(n, words.length); i++) {
            if (i > 0) sb.append(' ');
            sb.append(words[i]);
        }
        if (words.length > n) sb.append(" ...");
        return sb.toString();
    }

    static String parentAttr(Element el, String attr) {
        Node p = el.getParentNode();
        return (p instanceof Element) ? ((Element) p).getAttribute(attr) : "?";
    }

    private Document parseXml() throws Exception {
        try (InputStream is = getClass().getResourceAsStream(XML_PATH)) {
            assertNotNull("XML не найден: " + XML_PATH, is);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(is);
        }
    }

    private NodeList xpath(Document doc, String expression) throws Exception {
        XPathFactory xpf = XPathFactory.newInstance();
        return (NodeList) xpf.newXPath().evaluate(expression, doc, XPathConstants.NODESET);
    }
}
