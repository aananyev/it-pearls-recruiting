package com.company.hunttech.core;

import com.company.hunttech.dto.yandex.*;
import com.company.hunttech.entity.UserYandexConfiguration;
import com.company.hunttech.service.AiYandexOrchestrationService;
import com.company.hunttech.service.AiYandexOrchestrationServiceBean;
import com.company.hunttech.service.YandexIntegrationService;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Контрактные тесты для интеграции с сервисами Yandex 360 (Календарь, Телемост, Wiki, Почта):
 * 1. Целостность сущности UserYandexConfiguration и Liquibase миграций.
 * 2. Регистрация удаленных сервисов в web-spring.xml.
 * 3. Точность распознавания намерений AI: различение личного и корпоративного календаря («Hunttech у заказчика»).
 */
public class YandexIntegrationContractTest {

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
    public void testUserYandexConfigurationStructure() {
        assertEquals("https://caldav.yandex.ru", UserYandexConfiguration.DEFAULT_CALENDAR_BASE_URL);
        assertEquals("Hunttech у заказчика", UserYandexConfiguration.DEFAULT_CLIENT_CALENDAR_NAME);
        assertEquals("Europe/Saratov", UserYandexConfiguration.DEFAULT_TIME_ZONE);
        assertEquals("https://cloud-api.yandex.net/v1/telemost-api", UserYandexConfiguration.DEFAULT_TELEMOST_BASE_URL);

        UserYandexConfiguration config = new UserYandexConfiguration();
        config.setAccountEmail("alan@hunttech.ru");
        config.setCalendarConnected(true);
        config.setTelemostConnected(true);

        assertTrue(config.getCalendarConnected());
        assertTrue(config.getTelemostConnected());
        assertEquals("alan@hunttech.ru", config.getAccountEmail());
    }

    @Test
    public void testLiquibaseChangelogAndMasterRegistration() throws Exception {
        File changelog = resolveFile("modules/core/db/changelog/260913-3-add-user-yandex-configuration.xml");
        assertTrue("Файл миграции 260913-3-add-user-yandex-configuration.xml должен существовать", changelog.exists());

        String masterChangelog = readProjectFile("modules/core/db/changelog/db.changelog-master.xml");
        assertTrue("db.changelog-master.xml должен включать 260913-3-add-user-yandex-configuration.xml",
                masterChangelog.contains("260913-3-add-user-yandex-configuration.xml"));
    }

    @Test
    public void testViewsXmlRegistration() throws Exception {
        String viewsXml = readProjectFile("modules/global/src/com/company/hunttech/views.xml");
        assertTrue("views.xml должен содержать объявление сущности hunttech_UserYandexConfiguration",
                viewsXml.contains("entity=\"hunttech_UserYandexConfiguration\""));
        assertTrue("views.xml должен содержать view userYandexConfiguration-view",
                viewsXml.contains("name=\"userYandexConfiguration-view\""));
    }

    @Test
    public void testPersistenceXmlRegistration() throws Exception {
        String persistenceXml = readProjectFile("modules/global/src/com/company/hunttech/persistence.xml");
        assertTrue("persistence.xml должен содержать регистрацию класса UserYandexConfiguration",
                persistenceXml.contains("<class>com.company.hunttech.entity.UserYandexConfiguration</class>"));
    }

    @Test
    public void testWebSpringServiceRegistration() throws Exception {
        String webSpring = readProjectFile("modules/web/src/com/company/hunttech/web-spring.xml");
        assertTrue("web-spring.xml должен регистрировать remoteProxy hunttech_YandexIntegrationService",
                webSpring.contains("hunttech_YandexIntegrationService"));
        assertTrue("web-spring.xml должен регистрировать remoteProxy hunttech_AiYandexOrchestrationService",
                webSpring.contains("hunttech_AiYandexOrchestrationService"));
    }

    @Test
    public void testAiYandexIntentClassification() {
        AiYandexOrchestrationServiceBean orchestrationBean = new AiYandexOrchestrationServiceBean();

        // 1. Проверка намерения "в моем календаре"
        String personalMsg = "создай в моем календаре встречу с кандидатом Ивановым завтра в 15:00";
        assertTrue(orchestrationBean.isMeetingBookingIntent(personalMsg));
        AiMeetingParseResult personalResult = orchestrationBean.parseMeetingIntent(personalMsg, null);
        assertTrue(personalResult.isIntentDetected());
        assertEquals(YandexCalendarType.PERSONAL, personalResult.getCalendarType());
        assertTrue(personalResult.isTelemostRequired());
        assertNotNull(personalResult.getStartTime());

        // 2. Проверка намерения "в календаре собеседования с заказчиком"
        String clientMsg = "создай в календаре собеседования с заказчиком звонок с Ивановым Иваном послезавтра в 16:30";
        assertTrue(orchestrationBean.isMeetingBookingIntent(clientMsg));
        AiMeetingParseResult clientResult = orchestrationBean.parseMeetingIntent(clientMsg, null);
        assertTrue(clientResult.isIntentDetected());
        assertEquals(YandexCalendarType.CLIENT_INTERVIEW, clientResult.getCalendarType());
        assertEquals("Hunttech у заказчика", clientResult.getCalendarName());
        assertTrue(clientResult.isTelemostRequired());
        assertNotNull(clientResult.getStartTime());

        // 3. Нерелевантное сообщение
        String irrelevantMsg = "покажи список открытых вакансий";
        assertFalse(orchestrationBean.isMeetingBookingIntent(irrelevantMsg));

        // 4. Проверка точного запроса пользователя: личное событие с темой в кавычках, временем 12-00 и таймзоной Саратова
        String userRequest = "сделай в моем личном яндекс-календаре событие на завтра на 12-00 по саратовскому времени длительностью 1 час: \"Заняться медицинской страховкой\"";
        assertTrue("Должен распознаваться интент создания события", orchestrationBean.isMeetingBookingIntent(userRequest));
        AiMeetingParseResult userResult = orchestrationBean.parseMeetingIntent(userRequest, null);
        assertTrue("Интент должен быть определен", userResult.isIntentDetected());
        assertEquals(YandexCalendarType.PERSONAL, userResult.getCalendarType());
        assertEquals("Заняться медицинской страховкой", userResult.getTitle());
        assertEquals("Europe/Saratov", userResult.getTimeZone());
        assertFalse("Для личного события без кандидата Телемост не требуется", userResult.isTelemostRequired());

        java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Saratov"));
        cal.setTime(userResult.getStartTime());
        assertEquals(12, cal.get(java.util.Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(java.util.Calendar.MINUTE));

        long durationMillis = userResult.getEndTime().getTime() - userResult.getStartTime().getTime();
        assertEquals("Длительность должна составлять ровно 1 час (60 минут)", 60 * 60 * 1000L, durationMillis);
        assertTrue(userResult.getDescription().contains("Заняться медицинской страховкой"));
        assertTrue(userResult.getDescription().contains("Создано через HRM HuntTech (Яндекс 360)"));

        // 5. Проверка точки во времени (12.00) и темы через ключевое слово
        String dotTimeMsg = "поставь в моем календаре на завтра 12.00 тему: Встреча с тимлидом";
        assertTrue(orchestrationBean.isMeetingBookingIntent(dotTimeMsg));
        AiMeetingParseResult dotTimeResult = orchestrationBean.parseMeetingIntent(dotTimeMsg, null);
        assertTrue(dotTimeResult.isIntentDetected());
        assertEquals("Встреча с тимлидом", dotTimeResult.getTitle());

        // 6. Проверка "длительностью 2 часа" (120 минут)
        String duration2HoursMsg = "запланируй в моем календаре встречу на завтра длительностью 2 часа: \"Стратегическая сессия\"";
        assertTrue(orchestrationBean.isMeetingBookingIntent(duration2HoursMsg));
        AiMeetingParseResult durationResult = orchestrationBean.parseMeetingIntent(duration2HoursMsg, null);
        assertTrue(durationResult.isIntentDetected());
        assertEquals("Стратегическая сессия", durationResult.getTitle());
        long duration2HMillis = durationResult.getEndTime().getTime() - durationResult.getStartTime().getTime();
        assertEquals("Длительность должна быть 2 часа (120 минут)", 120 * 60 * 1000L, duration2HMillis);

        // 7. Проверка даты формата dd.MM.yyyy (например 13.09.2026), чтобы 09.20 не матчилась как время старта
        String dateNoTimeMsg = "поставь в моем календаре событие 13.09.2026: \"Планирование спринта\"";
        assertTrue(orchestrationBean.isMeetingBookingIntent(dateNoTimeMsg));
        AiMeetingParseResult dateNoTimeResult = orchestrationBean.parseMeetingIntent(dateNoTimeMsg, null);
        assertTrue(dateNoTimeResult.isIntentDetected());
        java.util.Calendar calDate = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(dateNoTimeResult.getTimeZone()));
        calDate.setTime(dateNoTimeResult.getStartTime());
        assertEquals("При отсутствии времени должен оставаться дефолтный час 15:00, а не 09:20", 15, calDate.get(java.util.Calendar.HOUR_OF_DAY));

        // 8. Проверка ISO-даты 2026-09-13, чтобы 09-13 не матчилась как время 09:13
        String isoDateMsg = "создай в моем календаре встречу 2026-09-13: \"Ретроспектива\"";
        assertTrue(orchestrationBean.isMeetingBookingIntent(isoDateMsg));
        AiMeetingParseResult isoDateResult = orchestrationBean.parseMeetingIntent(isoDateMsg, null);
        assertTrue(isoDateResult.isIntentDetected());
        java.util.Calendar calIso = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(isoDateResult.getTimeZone()));
        calIso.setTime(isoDateResult.getStartTime());
        assertEquals("ISO-дата не должна давать время 09:13, должен оставаться дефолтный час 15:00", 15, calIso.get(java.util.Calendar.HOUR_OF_DAY));

        // 9. Проверка темы без кавычек со стоп-словами предлогов
        String unquotedTopicMsg = "создай в моем календаре встречу на тему обсуждение бюджета на завтра в 16:00 с кандидатом Ивановым Иваном";
        assertTrue(orchestrationBean.isMeetingBookingIntent(unquotedTopicMsg));
        AiMeetingParseResult unquotedResult = orchestrationBean.parseMeetingIntent(unquotedTopicMsg, null);
        assertTrue(unquotedResult.isIntentDetected());
        assertTrue("Тема без кавычек должна отсекать дату/время", unquotedResult.getTitle().contains("обсуждение бюджета"));
        assertEquals("Ивановым Иваном", unquotedResult.getCandidateFio());

        // 10. Проверка сохранения номеров версий с точкой (например 'релиз версия 1.5')
        String versionTopicMsg = "поставь в моем календаре на завтра в 11:00 тему: релиз версия 1.5";
        assertTrue(orchestrationBean.isMeetingBookingIntent(versionTopicMsg));
        AiMeetingParseResult versionResult = orchestrationBean.parseMeetingIntent(versionTopicMsg, null);
        assertTrue(versionResult.isIntentDetected());
        assertTrue("Номер версии с точкой 1.5 не должен удаляться", versionResult.getTitle().contains("релиз версия 1.5"));

        // 11. Проверка "на 30 минут" как длительности
        String durationMinutesMsg = "создай в моем календаре встречу на завтра в 10:00 на 30 минут: \"Короткий синк\"";
        assertTrue(orchestrationBean.isMeetingBookingIntent(durationMinutesMsg));
        AiMeetingParseResult durationMinutesResult = orchestrationBean.parseMeetingIntent(durationMinutesMsg, null);
        assertTrue(durationMinutesResult.isIntentDetected());
        long duration30MinMillis = durationMinutesResult.getEndTime().getTime() - durationMinutesResult.getStartTime().getTime();
        assertEquals("Длительность должна быть 30 минут", 30 * 60 * 1000L, duration30MinMillis);

        // 12. Проверка, что 'на 5 человек' не парсится как 5 часов, а 'на 15 часов' парсится как время старта
        String timeOfDayMsg = "поставь в моем календаре встречу на завтра на 15 часов на 5 человек: \"Ревью\"";
        assertTrue(orchestrationBean.isMeetingBookingIntent(timeOfDayMsg));
        AiMeetingParseResult timeOfDayResult = orchestrationBean.parseMeetingIntent(timeOfDayMsg, null);
        assertTrue(timeOfDayResult.isIntentDetected());
        java.util.Calendar cal15H = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(timeOfDayResult.getTimeZone()));
        cal15H.setTime(timeOfDayResult.getStartTime());
        assertEquals("Время старта должно быть 15:00", 15, cal15H.get(java.util.Calendar.HOUR_OF_DAY));
        long defaultDurMillis = timeOfDayResult.getEndTime().getTime() - timeOfDayResult.getStartTime().getTime();
        assertEquals("Длительность должна остаться стандартной (60 минут), а не 300 минут", 60 * 60 * 1000L, defaultDurMillis);
    }

    @Test
    public void testCalendarQueryIntentRecognition() {
        AiYandexOrchestrationServiceBean orchestrationBean = new AiYandexOrchestrationServiceBean();

        // 1. Точная фраза пользователя из запроса (с опечатками: "янлекс календаь")
        String typoUserRequest = "посмотреть янлекс календаь";
        assertTrue("Должен распознаваться интент чтения календаря даже с опечатками",
                orchestrationBean.isCalendarQueryIntent(typoUserRequest));

        // 2. Стандартные запросы на просмотр
        assertTrue(orchestrationBean.isCalendarQueryIntent("посмотри яндекс календарь"));
        assertTrue(orchestrationBean.isCalendarQueryIntent("что у меня в календаре на сегодня"));
        assertTrue(orchestrationBean.isCalendarQueryIntent("какие встречи на следующей неделе"));
        assertTrue(orchestrationBean.isCalendarQueryIntent("покажи расписание на завтра"));
        assertTrue(orchestrationBean.isCalendarQueryIntent("проверь календарь на этой неделе"));
        assertTrue(orchestrationBean.isCalendarQueryIntent("глянь календарь"));
        assertTrue(orchestrationBean.isCalendarQueryIntent("какие события в календаре у заказчика"));
        assertTrue(orchestrationBean.isCalendarQueryIntent("что запланировано на понедельник"));

        // 3. Запросы на бронирование/создание НЕ должны классифицироваться как просмотр
        assertFalse(orchestrationBean.isCalendarQueryIntent("создай в календаре встречу с Ивановым завтра в 15:00"));
        assertFalse(orchestrationBean.isCalendarQueryIntent("запланируй в моем личном яндекс-календаре событие на 12-00"));
        assertFalse(orchestrationBean.isCalendarQueryIntent("поставь встречу с кандидатом"));

        // 4. Нерелевантные запросы
        assertFalse(orchestrationBean.isCalendarQueryIntent("покажи список открытых вакансий"));
        assertFalse(orchestrationBean.isCalendarQueryIntent("найди резюме Java разработчика"));
    }

    @Test
    public void testCaldavXmlAndIcsParsing() {
        String testXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<d:multistatus xmlns:d=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\">\n" +
                "  <d:response>\n" +
                "    <d:href>/calendars/alan%40hunttech.ru/events-34179601/event-123.ics</d:href>\n" +
                "    <d:propstat>\n" +
                "      <d:prop>\n" +
                "        <c:calendar-data>BEGIN:VCALENDAR\r\n" +
                "VERSION:2.0\r\n" +
                "PRODID:-//HUNTTECH//Recruiting HRM 1.0//RU\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:event-123-abc\r\n" +
                "DTSTART;TZID=Europe/Saratov:20260914T163000\r\n" +
                "DTEND;TZID=Europe/Saratov:20260914T173000\r\n" +
                "SUMMARY:Собеседование: Иванов Иван\r\n" +
                "DESCRIPTION:Техническое интервью на вакансию Senior Java Developer\\n\\n" +
                " Ссылка на видеовстречу Яндекс Телемост: https://telemost.yandex.ru/j/8899001122\r\n" +
                "LOCATION:https://telemost.yandex.ru/j/8899001122\r\n" +
                "ATTENDEE;CN=Иванов Иван:mailto:ivan@example.com\r\n" +
                "ATTENDEE;CN=Рекрутер:mailto:alan@hunttech.ru\r\n" +
                "STATUS:CONFIRMED\r\n" +
                "END:VEVENT\r\n" +
                "END:VCALENDAR</c:calendar-data>\n" +
                "      </d:prop>\n" +
                "      <d:status>HTTP/1.1 200 OK</d:status>\n" +
                "    </d:propstat>\n" +
                "  </d:response>\n" +
                "</d:multistatus>";

        java.util.TimeZone tz = java.util.TimeZone.getTimeZone("Europe/Saratov");
        java.util.List<YandexCalendarEventDto> events = com.company.hunttech.service.YandexIntegrationServiceBean
                .parseCaldavResponseXml(testXml, "Hunttech у заказчика", "/calendars/alan%40hunttech.ru/events-34179601/", tz);

        assertNotNull("Список событий не должен быть null", events);
        assertEquals("Должно распарситься ровно 1 событие", 1, events.size());

        YandexCalendarEventDto ev = events.get(0);
        assertEquals("event-123-abc", ev.getUid());
        assertEquals("Собеседование: Иванов Иван", ev.getSummary());
        assertEquals("Hunttech у заказчика", ev.getCalendarName());
        assertEquals("https://telemost.yandex.ru/j/8899001122", ev.getTelemostUrl());
        assertEquals("CONFIRMED", ev.getStatus());
        assertTrue(ev.getAttendees().contains("ivan@example.com"));
        assertTrue(ev.getAttendees().contains("alan@hunttech.ru"));

        java.util.Calendar cal = java.util.Calendar.getInstance(tz);
        cal.setTime(ev.getStartTime());
        assertEquals(2026, cal.get(java.util.Calendar.YEAR));
        assertEquals(java.util.Calendar.SEPTEMBER, cal.get(java.util.Calendar.MONTH));
        assertEquals(14, cal.get(java.util.Calendar.DAY_OF_MONTH));
        assertEquals(16, cal.get(java.util.Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(java.util.Calendar.MINUTE));

        assertEquals("16:30 – 17:30", ev.getFormattedTimeRange(tz));
    }

    @Test
    public void testAllWorkdaysTimeRangeAndMultipleSlotsParsing() {
        AiYandexOrchestrationServiceBean orchestrationBean = new AiYandexOrchestrationServiceBean();

        // 1. Каждый рабочий день на следующей неделе с 15 до 16
        String msg1 = "сделай в моем личном календаре на следующей неделе каждый рабочий день по саратовскому времени с 15 до 16 событие \"Забрать Аглаю из школы\"";
        assertTrue(orchestrationBean.isMeetingBookingIntent(msg1));

        AiMeetingParseResult result1 = orchestrationBean.parseMeetingIntent(msg1, null);
        assertTrue(result1.isIntentDetected());
        assertEquals("Забрать Аглаю из школы", result1.getTitle());
        assertEquals("Europe/Saratov", result1.getTimeZone());
        assertEquals(YandexCalendarType.PERSONAL, result1.getCalendarType());
        assertTrue("Для множественных слотов должен быть выставлен checkDuplicates", result1.isCheckDuplicates());
        assertNotNull(result1.getTimeSlots());
        assertEquals("Должно быть создано 5 слотов (пн-пт)", 5, result1.getTimeSlots().size());

        java.util.TimeZone tz = java.util.TimeZone.getTimeZone("Europe/Saratov");
        java.util.Calendar cal = java.util.Calendar.getInstance(tz);

        for (AiMeetingParseResult.TimeSlot slot : result1.getTimeSlots()) {
            cal.setTime(slot.getStartTime());
            assertEquals("Время начала должно быть 15:00", 15, cal.get(java.util.Calendar.HOUR_OF_DAY));
            assertEquals("Минуты начала должны быть 0", 0, cal.get(java.util.Calendar.MINUTE));

            cal.setTime(slot.getEndTime());
            assertEquals("Время окончания должно быть 16:00", 16, cal.get(java.util.Calendar.HOUR_OF_DAY));
            assertEquals("Минуты окончания должны быть 0", 0, cal.get(java.util.Calendar.MINUTE));

            long duration = slot.getEndTime().getTime() - slot.getStartTime().getTime();
            assertEquals("Длительность слота должна быть 1 час", 3600000L, duration);
        }

        // 2. Дни недели списком: в понедельник, среду, четверг и пятницу
        String msg2 = "сделай аналогичное событие в понедельник, среду, четверг и пятницу с 15 до 16";
        AiMeetingParseResult result2 = orchestrationBean.parseMeetingIntent(msg2, null);
        assertTrue(result2.isIntentDetected());
        assertEquals("Событие в календаре", result2.getTitle());
        assertEquals(4, result2.getTimeSlots().size());

        // 3. 18 числа с 15 до 16: число месяца не должно сбивать время на 18:00
        String msg3 = "поставь в моем календаре 18 числа с 15 до 16 Забрать Аглаю из школы";
        AiMeetingParseResult result3 = orchestrationBean.parseMeetingIntent(msg3, null);
        assertTrue(result3.isIntentDetected());
        assertEquals(1, result3.getTimeSlots().size());
        cal.setTime(result3.getTimeSlots().get(0).getStartTime());
        assertEquals("День месяца должен быть 18", 18, cal.get(java.util.Calendar.DAY_OF_MONTH));
        assertEquals("Время начала должно быть 15:00, а не 18:00!", 15, cal.get(java.util.Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(java.util.Calendar.MINUTE));
        cal.setTime(result3.getTimeSlots().get(0).getEndTime());
        assertEquals(16, cal.get(java.util.Calendar.HOUR_OF_DAY));

        // 4. в четверг с 15 до 16
        String msg4 = "создай в личном календаре в четверг с 15 до 16 событие \"Забрать Аглаю из школы\"";
        AiMeetingParseResult result4 = orchestrationBean.parseMeetingIntent(msg4, null);
        assertTrue(result4.isIntentDetected());
        assertEquals(1, result4.getTimeSlots().size());
        cal.setTime(result4.getTimeSlots().get(0).getStartTime());
        assertEquals("День недели должен быть четверг", java.util.Calendar.THURSDAY, cal.get(java.util.Calendar.DAY_OF_WEEK));
        assertEquals(15, cal.get(java.util.Calendar.HOUR_OF_DAY));
    }
}
