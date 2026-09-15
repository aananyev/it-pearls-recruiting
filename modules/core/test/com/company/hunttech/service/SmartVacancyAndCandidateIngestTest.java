package com.company.hunttech.service;

import com.company.hunttech.core.OpenPositionService;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.Iteraction;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class SmartVacancyAndCandidateIngestTest {

    private SmartOpenPositionIngestServiceBean vacancyService;
    private SmartCvIngestServiceBean cvService;

    @Before
    public void setUp() throws Exception {
        vacancyService = new SmartOpenPositionIngestServiceBean();
        cvService = new SmartCvIngestServiceBean();
    }

    @Test
    public void testFormatAsCleanHtmlConvertsMarkdownTable() {
        String mdTable = "### Чек-лист оценки\n" +
                "| № | Требование | Вопрос | Флаг |\n" +
                "|---|---|---|---|\n" +
                "| 1 | Java Core | Вопрос про GC | Знание generational GC |\n";

        String html = SmartOpenPositionIngestServiceBean.formatAsCleanHtml(mdTable);
        assertNotNull(html);
        assertTrue("Должен содержать тег <h3>", html.contains("<h3"));
        assertTrue("Должен содержать тег <table>", html.contains("<table"));
        assertTrue("Должен содержать thead", html.contains("<thead"));
        assertTrue("Должен содержать tbody", html.contains("<tbody"));
        assertTrue("Должен содержать th", html.contains("<th"));
        assertTrue("Должен содержать td", html.contains("<td"));
        assertFalse("Не должен содержать разделителей markdown таблиц", html.contains("|---|"));
    }

    @Test
    public void testFormatAsCleanHtmlConvertsListsAndHeaders() {
        String md = "#### Блок 1. Презентация\n" +
                "- Пункт 1\n" +
                "- Пункт 2\n" +
                "Обычный текст с **жирным** словом.";

        String html = SmartOpenPositionIngestServiceBean.formatAsCleanHtml(md);
        assertNotNull(html);
        assertTrue("Должен содержать h4", html.contains("<h4"));
        assertTrue("Должен содержать ul", html.contains("<ul"));
        assertTrue("Должен содержать li", html.contains("<li"));
        assertTrue("Должен содержать b для жирного шрифта", html.contains("<b>жирным</b>"));
    }

    @Test
    public void testFormatAsCleanHtmlPreservesValidHtml() {
        String inputHtml = "<h3>Заголовок</h3><p>Описание роли в проекте</p><ul><li>Навык 1</li><li>Навык 2</li></ul>";
        String outputHtml = SmartOpenPositionIngestServiceBean.formatAsCleanHtml(inputHtml);
        assertEquals(inputHtml, outputHtml);
    }

    @Test
    public void testFormatAsCleanHtmlSanitizesDangerousHtml() {
        String unsafeHtml = "<h3>Тест</h3><script>alert('xss')</script><iframe src='evil.com'></iframe><p>Нормальный текст</p>";
        String safeHtml = SmartOpenPositionIngestServiceBean.formatAsCleanHtml(unsafeHtml);
        assertNotNull(safeHtml);
        assertFalse("Не должен содержать тег script", safeHtml.contains("<script"));
        assertFalse("Не должен содержать тег iframe", safeHtml.contains("<iframe"));
        assertTrue("Должен сохранять безопасный текст", safeHtml.contains("<p>Нормальный текст</p>"));
    }

    @Test
    public void testFormatInlineMarkdownEscapesDangerousChars() throws Exception {
        Method formatMethod = SmartOpenPositionIngestServiceBean.class.getDeclaredMethod("formatInlineMarkdown", String.class);
        formatMethod.setAccessible(true);

        String raw = "Стек: **Java < 21 & Spring > 3** и `List<String>`";
        String formatted = (String) formatMethod.invoke(null, raw);
        assertNotNull(formatted);
        assertTrue("Должен содержать тег <b>", formatted.contains("<b>"));
        assertTrue("Должен экранировать < и >", formatted.contains("&lt;") && formatted.contains("&gt;"));
        assertTrue("Должен экранировать &", formatted.contains("&amp;"));
        assertFalse("Не должен содержать неэкранированных угловых скобок кроме тегов", formatted.contains("Java <"));
    }

    @Test
    public void testExtractVacancyIdFromUrlAndText() throws Exception {
        Method extMethod = SmartOpenPositionIngestServiceBean.class.getDeclaredMethod("extractVacancyIdFromTextOrUrl", String.class);
        extMethod.setAccessible(true);

        // URL HeadHunter
        String id1 = (String) extMethod.invoke(null, "Вакансия на hh: https://hh.ru/vacancy/11223344?query=java");
        assertEquals("11223344", id1);

        // Внутренний ID в тексте
        String id2 = (String) extMethod.invoke(null, "Заявка № 998877\nТребуется Java разработчик");
        assertEquals("998877", id2);

        // Req ID
        String id3 = (String) extMethod.invoke(null, "ID вакансии: REQ-1044\nСтек: Python");
        assertEquals("REQ-1044", id3);

        // Хэштег
        String id4 = (String) extMethod.invoke(null, "Срочно в команду #VAC-505 разработчик");
        assertEquals("VAC-505", id4);
    }

    @Test
    public void testCleanVacansyId() throws Exception {
        Method cleanMethod = SmartOpenPositionIngestServiceBean.class.getDeclaredMethod("cleanVacansyId", String.class);
        cleanMethod.setAccessible(true);

        String id1 = (String) cleanMethod.invoke(null, "№ 12345");
        assertEquals("12345", id1);

        String id2 = (String) cleanMethod.invoke(null, "https://hh.ru/vacancy/776655");
        assertEquals("776655", id2);

        String id3 = (String) cleanMethod.invoke(null, "VERY_LONG_VACANCY_IDENTIFIER_NUMBER_1234567890");
        assertTrue("Длина не должна превышать 16 символов", id3.length() <= 16);
    }

    @Test
    public void testSmartOpenPositionParsedDataHasVacansyID() {
        SmartOpenPositionParsedData data = new SmartOpenPositionParsedData();
        data.setVacansyID("REQ-7788");
        assertEquals("REQ-7788", data.getVacansyID());
    }

    @Test
    public void testSmartCvIngestInteractionResolutionWithDefaultVacancy() throws Exception {
        Metadata mockMetadata = (Metadata) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Metadata.class},
                (proxy, method, args) -> {
                    if ("create".equals(method.getName()) && args != null && args.length == 1) {
                        if (args[0] == IteractionList.class) return new IteractionList();
                        if (args[0] == Iteraction.class) return new Iteraction();
                        if (args[0] == OpenPosition.class) return new OpenPosition();
                    }
                    return null;
                }
        );

        OpenPosition defaultVacancy = new OpenPosition();
        defaultVacancy.setVacansyName("Default (Базовая вакансия)");

        OpenPositionService mockOpenPositionService = (OpenPositionService) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{OpenPositionService.class},
                (proxy, method, args) -> {
                    if ("getOpenPositionDefault".equals(method.getName())) {
                        return defaultVacancy;
                    }
                    return null;
                }
        );

        setField(cvService, "metadata", mockMetadata);
        setField(cvService, "openPositionService", mockOpenPositionService);

        Method resDefVacMethod = SmartCvIngestServiceBean.class.getDeclaredMethod("resolveDefaultVacancy");
        resDefVacMethod.setAccessible(true);
        OpenPosition resolvedVac = (OpenPosition) resDefVacMethod.invoke(cvService);
        assertNotNull("Вакансия по умолчанию должна быть разрешена", resolvedVac);
        assertEquals("Default (Базовая вакансия)", resolvedVac.getVacansyName());
    }

    @Test
    public void testFullVacancyParsedDataHtmlAndIdIntegration() throws Exception {
        SmartOpenPositionParsedData data = new SmartOpenPositionParsedData();
        data.setVacansyID("REQ-2026-99");
        data.setVacansyName("Senior Java Developer");
        data.setPositionTypeName("Разработчик Java");
        data.setComment("### Описание роли\nТребуется ведущий инженер.\n\n### Условия\nУдаленная работа.");
        data.setInterviewChecklist("| № | Навык | Вопрос |\n|---|---|---|\n| 1 | Spring | Вопрос по транзакциям |\n");
        data.setSearchMap("#### Поиск\n- Поиск на hh.ru\n- Поиск на LinkedIn\n");
        data.setInterviewPlan("### План собеседования\n1. Введение\n2. Техническая часть\n");

        // Проверяем, что форматирование в HTML дает чистую разметку
        String cleanComment = SmartOpenPositionIngestServiceBean.formatAsCleanHtml(data.getComment());
        assertTrue(cleanComment.contains("<h3"));
        assertFalse(cleanComment.contains("###"));

        String cleanChecklist = SmartOpenPositionIngestServiceBean.formatAsCleanHtml(data.getInterviewChecklist());
        assertTrue(cleanChecklist.contains("<table"));
        assertTrue(cleanChecklist.contains("<thead"));
        assertFalse(cleanChecklist.contains("|---|"));

        String cleanSearchMap = SmartOpenPositionIngestServiceBean.formatAsCleanHtml(data.getSearchMap());
        assertTrue(cleanSearchMap.contains("<h4"));
        assertTrue(cleanSearchMap.contains("<ul"));

        String cleanPlan = SmartOpenPositionIngestServiceBean.formatAsCleanHtml(data.getInterviewPlan());
        assertTrue(cleanPlan.contains("<h3"));
        assertTrue(cleanPlan.contains("<ol"));

        assertEquals("REQ-2026-99", data.getVacansyID());
        assertEquals("Senior Java Developer", data.getVacansyName());
        assertEquals("Разработчик Java", data.getPositionTypeName());
    }

    private static void setField(Object target, String fieldName, Object val) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, val);
    }
}
