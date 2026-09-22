package com.company.hunttech.core;

import com.company.hunttech.dto.yandex.YandexCalendarEventDto;
import com.company.hunttech.dto.yandex.YandexDiagnosticResult;
import com.company.hunttech.dto.yandex.YandexMeetingRequest;
import com.company.hunttech.dto.yandex.YandexMeetingResult;
import com.company.hunttech.entity.CorporateYandexCalendar;
import com.company.hunttech.entity.UserYandexConfiguration;
import com.company.hunttech.service.YandexIntegrationServiceBean;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Регрессионные тесты календарного подключения без обращения к реальному Яндекс Календарю.
 * Встроенный mock-CalDAV хранит только тестовое событие в памяти и проверяет обязательный cleanup.
 */
public class YandexCalendarConnectionTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final String CALENDAR_HOME_PATH = "/calendars/test%40example.invalid/";
    private static final String CALENDAR_PATH = "/calendars/test%40example.invalid/events/";

    private UserYandexConfiguration configuration;

    @Before
    public void setUp() {
        configuration = new UserYandexConfiguration();
        configuration.setAccountEmail("test@example.invalid");
        configuration.setOauthTokenEncrypted("encrypted-test-placeholder");
        configuration.setCalendarBaseUrl("https://caldav.test.invalid");
        configuration.setPersonalCalendarName("Автотест");
        configuration.setPersonalCalendarPath(CALENDAR_PATH);
        configuration.setDefaultTimeZone("Europe/Moscow");
    }

    @Test
    public void calendarDiagnosticsRejectsMissingToken() {
        StubYandexService service = new StubYandexService(configuration);
        service.token = null;

        YandexDiagnosticResult result = service.testConnection(USER_ID, "CALENDAR");

        assertFalse(result.isSuccess());
        assertEquals(401, result.getHttpStatusCode());
        assertFalse(Boolean.TRUE.equals(configuration.getCalendarConnected()));
        assertFalse(result.getMessage().contains("encrypted-test-placeholder"));
    }

    @Test
    public void calendarDiagnosticsRejectsUnauthorizedCaldavResponse() {
        StubYandexService service = new StubYandexService(configuration);
        service.discoveryStatus = 401;

        YandexDiagnosticResult result = service.testConnection(USER_ID, "CALENDAR");

        assertFalse(result.isSuccess());
        assertEquals(401, result.getHttpStatusCode());
        assertFalse(Boolean.TRUE.equals(configuration.getCalendarConnected()));
        assertNull("Ответ внешнего сервиса не должен попадать в диагностический артефакт", result.getDetails());
    }

    @Test
    public void calendarDiagnosticsRejectsNetworkFailure() {
        StubYandexService service = new StubYandexService(configuration);
        service.networkFailure = true;

        YandexDiagnosticResult result = service.testConnection(USER_ID, "CALENDAR");

        assertFalse(result.isSuccess());
        assertEquals(503, result.getHttpStatusCode());
        assertFalse(Boolean.TRUE.equals(configuration.getCalendarConnected()));
    }

    @Test
    public void calendarDiagnosticsAcceptsSuccessfulCaldavDiscovery() {
        StubYandexService service = new StubYandexService(configuration);

        YandexDiagnosticResult result = service.testConnection(USER_ID, "CALENDAR");

        assertTrue(result.getMessage(), result.isSuccess());
        assertEquals(200, result.getHttpStatusCode());
        assertTrue(Boolean.TRUE.equals(configuration.getCalendarConnected()));
        assertTrue(result.getMessage().contains("1 календар"));
    }

    @Test
    public void mockCaldavSupportsCreateReadUpdateDeleteAndCleanup() {
        StubYandexService service = new StubYandexService(configuration);
        YandexMeetingRequest request = meetingRequest("Календарный CRUD: создание");

        YandexMeetingResult created = service.scheduleCalendarEvent(USER_ID, request);
        assertTrue(created.getMessage(), created.isSuccess());
        assertNotNull(created.getEventUid());
        assertEquals(1, service.events.size());

        List<YandexCalendarEventDto> events = service.getCalendarEvents(
                USER_ID, CALENDAR_PATH, new Date(0), new Date(Long.MAX_VALUE));
        assertEquals(1, events.size());
        assertEquals("Календарный CRUD: создание", events.get(0).getSummary());

        request.setEventUid(created.getEventUid());
        request.setTitle("Календарный CRUD: обновление");
        YandexMeetingResult updated = service.scheduleCalendarEvent(USER_ID, request);
        assertTrue(updated.getMessage(), updated.isSuccess());
        assertEquals(created.getEventUid(), updated.getEventUid());
        assertEquals(1, service.events.size());
        assertTrue(service.events.values().iterator().next().contains("SUMMARY:Календарный CRUD: обновление"));

        assertTrue(service.cancelCalendarEvent(USER_ID, CALENDAR_PATH, created.getEventUid()));
        assertTrue("Тестовое событие должно быть удалено после CRUD-сценария", service.events.isEmpty());
    }

    private YandexMeetingRequest meetingRequest(String title) {
        YandexMeetingRequest request = new YandexMeetingRequest();
        request.setTitle(title);
        request.setDescription("Изолированное событие автоматизированного теста");
        request.setStartTime(new Date(1_800_000_000_000L));
        request.setEndTime(new Date(1_800_003_600_000L));
        request.setTimeZone("Europe/Moscow");
        request.setCreateTelemostMeeting(false);
        return request;
    }

    private static class StubYandexService extends YandexIntegrationServiceBean {
        private final UserYandexConfiguration configuration;
        private final Map<String, String> events = new LinkedHashMap<>();
        private String token = "test-token-placeholder";
        private int discoveryStatus = 207;
        private boolean networkFailure;

        private StubYandexService(UserYandexConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public UserYandexConfiguration getOrCreateConfiguration(UUID userId) {
            return configuration;
        }

        @Override
        protected String resolveToken(UserYandexConfiguration config) {
            return token;
        }

        @Override
        protected void updateCalendarDiagnosticState(UserYandexConfiguration config, String serviceType,
                                                     boolean connected, String message) {
            config.setCalendarConnected(connected);
            config.setLastVerifiedAt(new Date());
            config.setLastVerificationMessage(message);
        }

        @Override
        protected CorporateYandexCalendar resolveCorporateCalendarForPath(String calendarPath) {
            return null;
        }

        @Override
        protected HttpResult sendHttp(String method, String url, String authToken, String body,
                                      String contentType, Map<String, String> headers) throws Exception {
            if (networkFailure) {
                throw new IOException("simulated network failure");
            }
            if ("PROPFIND".equals(method)) {
                if (discoveryStatus != 207) {
                    return new HttpResult(discoveryStatus, "sensitive-provider-response");
                }
                if (url.contains("/principals/")) {
                    return new HttpResult(207,
                            "<d:multistatus xmlns:d=\"DAV:\"><d:response><d:href>" + CALENDAR_HOME_PATH
                                    + "</d:href></d:response></d:multistatus>");
                }
                return new HttpResult(207,
                        "<d:multistatus xmlns:d=\"DAV:\"><d:response><d:href>" + CALENDAR_PATH
                                + "</d:href><d:displayname>Автотест</d:displayname>"
                                + "<d:resourcetype><d:collection/><c:calendar xmlns:c=\"urn:ietf:params:xml:ns:caldav\"/>"
                                + "</d:resourcetype></d:response></d:multistatus>");
            }
            if ("PUT".equals(method)) {
                boolean existed = events.containsKey(url);
                events.put(url, body);
                return new HttpResult(existed ? 204 : 201, "");
            }
            if ("GET".equals(method)) {
                String event = events.get(url);
                return new HttpResult(event == null ? 404 : 200, event == null ? "" : event);
            }
            if ("REPORT".equals(method)) {
                StringBuilder xml = new StringBuilder("<d:multistatus xmlns:d=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\">");
                for (String event : new ArrayList<>(events.values())) {
                    xml.append("<d:response><c:calendar-data>")
                            .append(event)
                            .append("</c:calendar-data></d:response>");
                }
                return new HttpResult(207, xml.append("</d:multistatus>").toString());
            }
            if ("DELETE".equals(method)) {
                return new HttpResult(events.remove(url) == null ? 404 : 204, "");
            }
            return new HttpResult(405, "");
        }
    }
}
