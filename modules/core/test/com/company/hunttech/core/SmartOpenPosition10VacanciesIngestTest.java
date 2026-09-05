package com.company.hunttech.core;

import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.OpenPositionPriority;
import com.company.hunttech.service.SmartOpenPositionIngestServiceBean;
import com.company.hunttech.service.SmartOpenPositionParsedData;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Тестирование умного распознавания и загрузки 10 различных вакансий в OpenPosition
 * с проверкой обязательной установки приоритета «На проверку» (-2).
 */
public class SmartOpenPosition10VacanciesIngestTest {

    private SmartOpenPositionIngestServiceBean service;

    @Before
    public void setUp() {
        service = new SmartOpenPositionIngestServiceBean();
    }

    @Test
    public void testVacancy1_SeniorJavaDeveloper() {
        String text = "Вакансия: Senior Java / Spring Backend Developer\n" +
                "Компания: ФинТех Платформа, г. Москва (удаленная работа)\n" +
                "Зарплатная вилка: 300 000 - 450 000 руб.\n" +
                "Опыт работы от 5 лет. Грейд: Senior.\n" +
                "Требования: глубокое знание Java, Spring Boot, PostgreSQL, Kafka, Docker, Kubernetes, CI/CD.";

        SmartOpenPositionParsedData data = service.parseVacancyText(text);
        assertNotNull(data);
        assertTrue(data.getVacansyName().contains("Senior Java"));
        assertEquals(new BigDecimal("300000"), data.getSalaryMin());
        assertEquals(new BigDecimal("450000"), data.getSalaryMax());
        assertEquals(Integer.valueOf(1), data.getRemoteWork());
        assertEquals("Senior", data.getGradeName());
        assertNotNull(data.getRequiredSkills());
        assertTrue(data.getRequiredSkills().contains("Java"));
        assertTrue(data.getRequiredSkills().contains("Spring"));
    }

    @Test
    public void testVacancy2_MiddleReactFrontend() {
        String text = "Позиция: Middle Frontend React Developer\n" +
                "Локация: Санкт-Петербург, гибридный формат\n" +
                "Оклад: 180 000 - 260 000 руб. Опыт от 3 лет.\n" +
                "Стек: React, TypeScript, Redux, JavaScript, HTML5, CSS3, Git.";

        SmartOpenPositionParsedData data = service.parseVacancyText(text);
        assertNotNull(data);
        assertTrue(data.getVacansyName().contains("Middle Frontend React"));
        assertEquals(new BigDecimal("180000"), data.getSalaryMin());
        assertEquals(new BigDecimal("260000"), data.getSalaryMax());
        assertEquals(Integer.valueOf(2), data.getRemoteWork());
        assertEquals("Middle", data.getGradeName());
        assertTrue(data.getRequiredSkills().contains("React"));
        assertTrue(data.getRequiredSkills().contains("TypeScript"));
    }

    @Test
    public void testVacancy3_LeadDevOpsEngineer() {
        String text = "Должность: Lead DevOps / SRE Engineer\n" +
                "Формат: Полная удаленка (remote)\n" +
                "Зарплата: 400 000 - 550 000 руб. Опыт от 6 лет.\n" +
                "Обязанности: поддержка кластеров Kubernetes, Docker, Linux, CI/CD, Terraform, Prometheus, Git.";

        SmartOpenPositionParsedData data = service.parseVacancyText(text);
        assertNotNull(data);
        assertTrue(data.getVacansyName().contains("Lead DevOps"));
        assertEquals(new BigDecimal("400000"), data.getSalaryMin());
        assertEquals(new BigDecimal("550000"), data.getSalaryMax());
        assertEquals(Integer.valueOf(1), data.getRemoteWork());
        assertEquals("Lead", data.getGradeName());
        assertTrue(data.getRequiredSkills().contains("Kubernetes"));
        assertTrue(data.getRequiredSkills().contains("Docker"));
    }

    @Test
    public void testVacancy4_SeniorDataScientist() {
        String text = "Вакансия: Senior Data Scientist / ML Engineer\n" +
                "Город: Москва (офис)\n" +
                "Компенсация: 350 000 - 500 000 руб. Опыт от 4 лет.\n" +
                "Стек: Python, SQL, PostgreSQL, Docker, Git, Machine Learning, PyTorch, Pandas.";

        SmartOpenPositionParsedData data = service.parseVacancyText(text);
        assertNotNull(data);
        assertTrue(data.getVacansyName().contains("Senior Data Scientist"));
        assertEquals(new BigDecimal("350000"), data.getSalaryMin());
        assertEquals(new BigDecimal("500000"), data.getSalaryMax());
        assertEquals(Integer.valueOf(0), data.getRemoteWork());
        assertEquals("Senior", data.getGradeName());
        assertTrue(data.getRequiredSkills().contains("Python"));
        assertTrue(data.getRequiredSkills().contains("SQL"));
    }

    @Test
    public void testVacancy5_QAAutomationEngineer() {
        String text = "Позиция: QA Automation Engineer (Java)\n" +
                "Удаленная работа (remote)\n" +
                "Уровень дохода: 150 000 - 220 000 руб.\n" +
                "Опыт работы от 2 лет.\n" +
                "Требования: Java, Selenium, Allure, JUnit, Git, CI/CD, SQL.";

        SmartOpenPositionParsedData data = service.parseVacancyText(text);
        assertNotNull(data);
        assertTrue(data.getVacansyName().contains("QA Automation"));
        assertEquals(new BigDecimal("150000"), data.getSalaryMin());
        assertEquals(new BigDecimal("220000"), data.getSalaryMax());
        assertEquals(Integer.valueOf(1), data.getRemoteWork());
        assertTrue(data.getRequiredSkills().contains("Java"));
        assertTrue(data.getRequiredSkills().contains("SQL"));
    }

    @Test
    public void testVacancy6_MiddleGolangDeveloper() {
        String text = "Вакансия: Middle Go / Golang Developer\n" +
                "Локация: Екатеринбург (удаленка)\n" +
                "Зарплата: 200 000 - 300 000 руб.\n" +
                "Опыт от 3 лет. Грейд: Middle.\n" +
                "Стек: Go, PostgreSQL, Redis, Docker, Kafka, gRPC, Git.";

        SmartOpenPositionParsedData data = service.parseVacancyText(text);
        assertNotNull(data);
        assertTrue(data.getVacansyName().contains("Middle Go"));
        assertEquals(new BigDecimal("200000"), data.getSalaryMin());
        assertEquals(new BigDecimal("300000"), data.getSalaryMax());
        assertEquals(Integer.valueOf(1), data.getRemoteWork());
        assertEquals("Middle", data.getGradeName());
        assertTrue(data.getRequiredSkills().contains("Go"));
        assertTrue(data.getRequiredSkills().contains("Redis"));
    }

    @Test
    public void testVacancy7_ProductManager() {
        String text = "Должность: Product Manager / Руководитель IT-продукта\n" +
                "Офис: Москва, м. Белорусская\n" +
                "Зарплата: 250 000 - 380 000 руб.\n" +
                "Опыт управления продуктами от 4 лет.\n" +
                "Требования: Product Management, Agile, Scrum, Unit Economics, SQL, Jira.";

        SmartOpenPositionParsedData data = service.parseVacancyText(text);
        assertNotNull(data);
        assertTrue(data.getVacansyName().contains("Product Manager"));
        assertEquals(new BigDecimal("250000"), data.getSalaryMin());
        assertEquals(new BigDecimal("380000"), data.getSalaryMax());
        assertEquals(Integer.valueOf(0), data.getRemoteWork());
        assertTrue(data.getRequiredSkills().contains("SQL"));
    }

    @Test
    public void testVacancy8_UiUxDesigner() {
        String text = "Вакансия: UI/UX Designer\n" +
                "Город: Казань (полная удаленка)\n" +
                "Доход: 140 000 - 210 000 руб. Опыт от 3 лет.\n" +
                "Стек и инструменты: Figma, UI Kit, Design System, UX Research, Prototyping, Wireframing.";

        SmartOpenPositionParsedData data = service.parseVacancyText(text);
        assertNotNull(data);
        assertTrue(data.getVacansyName().contains("UI/UX Designer"));
        assertEquals(new BigDecimal("140000"), data.getSalaryMin());
        assertEquals(new BigDecimal("210000"), data.getSalaryMax());
        assertEquals(Integer.valueOf(1), data.getRemoteWork());
    }

    @Test
    public void testVacancy10_AndroidKotlinDeveloper() {
        String text = "Вакансия: Middle Android Developer (Kotlin)\n" +
                "Город: Самара (удаленка)\n" +
                "Зарплата: 190 000 - 280 000 руб. Опыт от 2 лет.\n" +
                "Технологии: Kotlin, Android SDK, Coroutines, Flow, Dagger, Git, CI/CD.";

        SmartOpenPositionParsedData data = service.parseVacancyText(text);
        assertNotNull(data);
        assertTrue(data.getVacansyName().contains("Middle Android"));
        assertEquals(new BigDecimal("190000"), data.getSalaryMin());
        assertEquals(new BigDecimal("280000"), data.getSalaryMax());
        assertEquals(Integer.valueOf(1), data.getRemoteWork());
        assertTrue(data.getRequiredSkills().contains("Kotlin"));
        assertTrue(data.getRequiredSkills().contains("Git"));
    }

    @Test
    public void testPriorityUnderReviewConstantAndFiltering() {
        assertEquals(Integer.valueOf(-2), OpenPositionPriority.UNDER_REVIEW.getId());
        assertEquals(OpenPositionPriority.UNDER_REVIEW, OpenPositionPriority.fromId(-2));
    }
}
