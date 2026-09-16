package com.company.hunttech.core;

import com.company.hunttech.dto.action.InterviewSchedulingIntent;
import com.company.hunttech.dto.action.InterviewSchedulingResult;
import com.company.hunttech.dto.action.PendingInterviewState;
import com.company.hunttech.entity.*;
import com.company.hunttech.entity.ai.LlmChatPendingAction;
import com.company.hunttech.service.InterviewSchedulingActionService;
import com.company.hunttech.service.InterviewSchedulingActionServiceBean;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Комплексные контрактные тесты для InterviewSchedulingActionService:
 * 1. Проверка регистрации Liquibase миграций таблицы HUNTTECH_LLM_CHAT_PENDING_ACTION и seed-миграции INTERVIEW_SCHEDULING_PARSE.
 * 2. Проверка регистрации сущности LlmChatPendingAction в persistence.xml и views.xml.
 * 3. Проверка регистрации прокси hunttech_InterviewSchedulingActionService в web-spring.xml.
 * 4. Точность распознавания интента назначения собеседования кандидату.
 * 5. Распознавание проектов по владельцу («в проект Дениса Гаркушина») и по наименованию («в проект ВТБ»).
 * 6. Точность вычисления даты и времени с учетом временных зон («Europe/Moscow», «Europe/Saratov»).
 */
public class InterviewSchedulingActionContractTest {

    private File resolveFile(String relativePath) {
        File file = new File(relativePath);
        if (file.exists()) {
            return file;
        }
        File subFile = new File("../../" + relativePath);
        if (subFile.exists()) {
            return subFile;
        }
        File coreFile = new File("../" + relativePath);
        if (coreFile.exists()) {
            return coreFile;
        }
        return file;
    }

    private String readProjectFile(String relativePath) throws Exception {
        File file = resolveFile(relativePath);
        assertTrue("Файл должен существовать: " + relativePath, file.exists());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void testLiquibaseAndPendingActionMigration() throws Exception {
        File changelog = resolveFile("modules/core/db/changelog/260916-1-addInterviewSchedulingActionAndPendingTable.xml");
        assertTrue("Файл миграции 260916-1 должен существовать", changelog.exists());

        String masterChangelog = readProjectFile("modules/core/db/changelog/db.changelog-master.xml");
        assertTrue("db.changelog-master.xml должен включать 260916-1-addInterviewSchedulingActionAndPendingTable.xml",
                masterChangelog.contains("260916-1-addInterviewSchedulingActionAndPendingTable.xml"));

        String changelogContent = readProjectFile("modules/core/db/changelog/260916-1-addInterviewSchedulingActionAndPendingTable.xml");
        assertTrue("Changelog должен создавать таблицу HUNTTECH_LLM_CHAT_PENDING_ACTION",
                changelogContent.contains("tableName=\"HUNTTECH_LLM_CHAT_PENDING_ACTION\""));
        assertTrue("Changelog должен засевать AI функцию INTERVIEW_SCHEDULING_PARSE",
                changelogContent.contains("'INTERVIEW_SCHEDULING_PARSE'"));
    }

    @Test
    public void testPersistenceAndViewsRegistration() throws Exception {
        String persistenceXml = readProjectFile("modules/global/src/com/company/hunttech/persistence.xml");
        assertTrue("persistence.xml должен содержать com.company.hunttech.entity.ai.LlmChatPendingAction",
                persistenceXml.contains("<class>com.company.hunttech.entity.ai.LlmChatPendingAction</class>"));

        String viewsXml = readProjectFile("modules/global/src/com/company/hunttech/views.xml");
        assertTrue("views.xml должен содержать view llmChatPendingAction-view",
                viewsXml.contains("name=\"llmChatPendingAction-view\""));

        String webSpringXml = readProjectFile("modules/web/src/com/company/hunttech/web-spring.xml");
        assertTrue("web-spring.xml должен регистрировать прокси hunttech_InterviewSchedulingActionService",
                webSpringXml.contains("key=\"hunttech_InterviewSchedulingActionService\""));
    }

    @Test
    public void testIntentRecognitionAndHeuristics() {
        InterviewSchedulingActionServiceBean bean = new InterviewSchedulingActionServiceBean();

        // 1. Позитивный сценарий: проект Дениса Гаркушина
        String msgGarkushin = "назначь собеседование для Эльчина Аббасова на вакансию Системного аналитика в проект Дениса Гаркушина на завтра на 17-00 по Москве.";
        assertTrue("Сообщение должно определяться как интент назначения собеседования",
                bean.isInterviewSchedulingIntent(msgGarkushin));

        InterviewSchedulingIntent intentGarkushin = bean.parseSchedulingIntent(msgGarkushin, null);
        assertTrue(intentGarkushin.isIntentDetected());
        assertEquals("Эльчина Аббасова", intentGarkushin.getCandidateQuery());
        assertEquals("Системного аналитика", intentGarkushin.getVacancyQuery());
        assertEquals("Денис Гаркушин", intentGarkushin.getProjectOwnerQuery());
        assertNull("projectQuery должен быть null, так как распознан projectOwnerQuery", intentGarkushin.getProjectQuery());
        assertEquals("Europe/Moscow", intentGarkushin.getTimeZone());

        assertNotNull("Целевая дата и время должны быть вычислены", intentGarkushin.getTargetDateTime());
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        cal.setTime(intentGarkushin.getTargetDateTime());
        assertEquals(17, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));

        // 2. Позитивный сценарий: проект ВТБ
        String msgVtb = "назначь собеседование для Эльчина Аббасова на вакансию Системного аналитика в проект ВТБ на завтра на 17-00";
        assertTrue(bean.isInterviewSchedulingIntent(msgVtb));

        InterviewSchedulingIntent intentVtb = bean.parseSchedulingIntent(msgVtb, null);
        assertTrue(intentVtb.isIntentDetected());
        assertEquals("Эльчина Аббасова", intentVtb.getCandidateQuery());
        assertEquals("Системного аналитика", intentVtb.getVacancyQuery());
        assertEquals("ВТБ", intentVtb.getProjectQuery());
        assertNull(intentVtb.getProjectOwnerQuery());

        // 3. Негативные сценарии (не должны определяться как назначение собеседования)
        assertFalse(bean.isInterviewSchedulingIntent("Привет, как дела?"));
        assertFalse(bean.isInterviewSchedulingIntent("Покажи список открытых вакансий"));
        assertFalse(bean.isInterviewSchedulingIntent("Какая погода в Саратове?"));
        assertFalse(bean.isInterviewSchedulingIntent("Удали кандидата Иванова"));
    }

    @Test
    public void testPendingInterviewStateIntegrity() {
        PendingInterviewState state = new PendingInterviewState();
        state.setStep("NEED_VACANCY");
        state.setCandidateFio("Эльчин Аббасов");
        state.setCandidateEmail("elchin@example.com");
        state.setProjectName("ВТБ");
        state.setTimeZone("Europe/Moscow");

        Date now = new Date();
        state.setTargetDateTime(now);

        assertEquals("NEED_VACANCY", state.getStep());
        assertEquals("Эльчин Аббасов", state.getCandidateFio());
        assertEquals("elchin@example.com", state.getCandidateEmail());
        assertEquals("ВТБ", state.getProjectName());
        assertEquals(now, state.getTargetDateTime());
    }

    @Test
    public void testDateTimeResolutionCases() {
        InterviewSchedulingActionServiceBean bean = new InterviewSchedulingActionServiceBean();

        // Тест 1: Запрос без даты и времени должен давать targetDateTime = null
        String msgNoDate = "назначь собеседование для Эльчина Аббасова на вакансию Системного аналитика в проект ВТБ";
        InterviewSchedulingIntent intentNoDate = bean.parseSchedulingIntent(msgNoDate, null);
        assertNull("Когда дата и время не переданы, targetDateTime должен быть null", intentNoDate.getTargetDateTime());

        // Тест 2: Запрос с явным временем "завтра на 15:30"
        String msgTomorrow = "собеседование для Эльчина Аббасова на завтра в 15:30";
        InterviewSchedulingIntent intentTomorrow = bean.parseSchedulingIntent(msgTomorrow, null);
        assertNotNull(intentTomorrow.getTargetDateTime());
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        cal.setTime(intentTomorrow.getTargetDateTime());
        assertEquals(15, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));

        // Тест 3: Запрос с временем через точку "на 18.00"
        String msgDotTime = "назначь интервью с кандидатом Ивановым на завтра в 18.00";
        InterviewSchedulingIntent intentDot = bean.parseSchedulingIntent(msgDotTime, null);
        assertNotNull(intentDot.getTargetDateTime());
        cal.setTime(intentDot.getTargetDateTime());
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
    }

    @Test
    public void testSchedulingResultIntegrity() {
        // Pending результат должен иметь success = false и pendingUserClarification = true
        com.company.hunttech.dto.action.InterviewSchedulingResult pending =
                com.company.hunttech.dto.action.InterviewSchedulingResult.pending("NEED_VACANCY", "Выберите вакансию");
        assertFalse(pending.isSuccess());
        assertTrue(pending.isPendingUserClarification());
        assertEquals("NEED_VACANCY", pending.getPendingStep());

        // Success результат
        com.company.hunttech.dto.action.InterviewSchedulingResult success =
                com.company.hunttech.dto.action.InterviewSchedulingResult.success("Встреча назначена", java.util.UUID.randomUUID(), "https://telemost.yandex.ru/j/123", "uid-123", "Основной");
        assertTrue(success.isSuccess());
        assertFalse(success.isPendingUserClarification());
        assertEquals("NONE", success.getPendingStep());
        assertEquals("https://telemost.yandex.ru/j/123", success.getTelemostJoinUrl());
    }
}
