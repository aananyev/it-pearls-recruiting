package com.company.hunttech.core;

import com.company.hunttech.core.ai.AiSecretService;
import com.company.hunttech.dto.yandex.YandexDiagnosticResult;
import com.company.hunttech.entity.UserYandexConfiguration;
import com.company.hunttech.service.YandexIntegrationServiceBean;
import com.haulmont.cuba.core.global.DataManager;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Проверяет диагностический контракт подключения к Яндекс.Календарю без внешней сети
 * и без использования реальных OAuth-токенов.
 */
public class YandexCalendarConnectionDiagnosticsTest {

    private UserYandexConfiguration configuration;
    private StubYandexIntegrationService service;

    @Before
    public void setUp() throws Exception {
        configuration = new UserYandexConfiguration();
        configuration.setAccountEmail("calendar-test@example.invalid");
        configuration.setOauthTokenEncrypted("encrypted-test-token");
        configuration.setCalendarBaseUrl("https://caldav.example.invalid");

        DataManager dataManager = mock(DataManager.class);
        when(dataManager.commit(any(UserYandexConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AiSecretService secretService = mock(AiSecretService.class);
        when(secretService.decrypt("encrypted-test-token")).thenReturn("test-token");

        service = new StubYandexIntegrationService(configuration);
        setField(service, "dataManager", dataManager);
        setField(service, "aiSecretService", secretService);
    }

    @Test
    public void successfulDiscoveryMarksCalendarConnected() {
        service.enqueue(207, principalResponse("/calendars/calendar-test%40example.invalid/"));
        service.enqueue(207, calendarListResponse());

        YandexDiagnosticResult result = service.testConnection(UUID.randomUUID(), "CALENDAR");

        assertTrue(result.isSuccess());
        assertEquals(200, result.getHttpStatusCode());
        assertTrue(configuration.getCalendarConnected());
        assertNotNull(configuration.getLastVerifiedAt());
        assertTrue(result.getMessage().contains("Обнаружено 1 календарей"));
    }

    @Test
    public void expiredOrInvalidTokenReturnsUnauthorized() {
        service.enqueue(401, "Unauthorized");

        YandexDiagnosticResult result = service.testConnection(UUID.randomUUID(), "CALENDAR");

        assertFalse(result.isSuccess());
        assertEquals(401, result.getHttpStatusCode());
        assertFalse(configuration.getCalendarConnected());
        assertNull("Тело ответа поставщика не должно попадать в диагностический результат", result.getDetails());
    }

    @Test
    public void insufficientCalendarScopeReturnsForbidden() {
        service.enqueue(403, "Forbidden");

        YandexDiagnosticResult result = service.testConnection(UUID.randomUUID(), "CALENDAR");

        assertFalse(result.isSuccess());
        assertEquals(403, result.getHttpStatusCode());
        assertTrue(result.getMessage().contains("HTTP 403"));
        assertFalse(configuration.getCalendarConnected());
    }

    @Test
    public void unavailableCalendarAccountReturnsNotFound() {
        service.enqueue(404, "Not found");

        YandexDiagnosticResult result = service.testConnection(UUID.randomUUID(), "CALENDAR");

        assertFalse(result.isSuccess());
        assertEquals(404, result.getHttpStatusCode());
        assertTrue(result.getMessage().contains("HTTP 404"));
        assertFalse(configuration.getCalendarConnected());
    }

    @Test
    public void emptySuccessfulDiscoveryDoesNotInventCalendarPaths() {
        service.enqueue(207, principalResponse("/calendars/calendar-test%40example.invalid/"));
        service.enqueue(207, "<?xml version=\"1.0\"?><d:multistatus xmlns:d=\"DAV:\"/>");

        YandexDiagnosticResult result = service.testConnection(UUID.randomUUID(), "CALENDAR");

        assertFalse(result.isSuccess());
        assertEquals(404, result.getHttpStatusCode());
        assertFalse(configuration.getCalendarConnected());
    }

    @Test
    public void networkTimeoutReturnsServiceUnavailable() {
        service.failWith(new SocketTimeoutException("synthetic timeout"));

        YandexDiagnosticResult result = service.testConnection(UUID.randomUUID(), "CALENDAR");

        assertFalse(result.isSuccess());
        assertEquals(503, result.getHttpStatusCode());
        assertEquals("Ошибка календарного подключения: сетевая ошибка CalDAV", result.getMessage());
        assertFalse(configuration.getCalendarConnected());
        assertFalse("Техническая причина не должна раскрывать секреты в details",
                result.getDetails() != null && result.getDetails().contains("test-token"));
    }

    private static String principalResponse(String homePath) {
        return "<?xml version=\"1.0\"?><d:multistatus xmlns:d=\"DAV:\" "
                + "xmlns:c=\"urn:ietf:params:xml:ns:caldav\"><d:response><d:propstat><d:prop>"
                + "<c:calendar-home-set><d:href>" + homePath + "</d:href></c:calendar-home-set>"
                + "</d:prop></d:propstat></d:response></d:multistatus>";
    }

    private static String calendarListResponse() {
        return "<?xml version=\"1.0\"?><d:multistatus xmlns:d=\"DAV:\" "
                + "xmlns:c=\"urn:ietf:params:xml:ns:caldav\"><d:response>"
                + "<d:href>/calendars/calendar-test%40example.invalid/events/</d:href>"
                + "<d:propstat><d:prop><d:displayname>Основной</d:displayname>"
                + "<d:resourcetype><d:collection/><c:calendar/></d:resourcetype>"
                + "</d:prop></d:propstat></d:response></d:multistatus>";
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = YandexIntegrationServiceBean.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class StubYandexIntegrationService extends YandexIntegrationServiceBean {
        private final UserYandexConfiguration configuration;
        private final Queue<HttpResult> responses = new ArrayDeque<>();
        private Exception failure;

        private StubYandexIntegrationService(UserYandexConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public UserYandexConfiguration getOrCreateConfiguration(UUID userId) {
            return configuration;
        }

        private void enqueue(int statusCode, String body) {
            responses.add(new HttpResult(statusCode, body));
        }

        private void failWith(Exception exception) {
            failure = exception;
        }

        @Override
        protected HttpResult sendHttp(String method, String url, String token, String body,
                                      String contentType, Map<String, String> headers) throws Exception {
            // Транспортный дубль не пишет token в сообщения и обеспечивает детерминированные ответы.
            if (failure != null) {
                throw failure;
            }
            HttpResult response = responses.poll();
            if (response == null) {
                throw new AssertionError("Не подготовлен ответ для " + method + " " + url);
            }
            return response;
        }
    }
}
