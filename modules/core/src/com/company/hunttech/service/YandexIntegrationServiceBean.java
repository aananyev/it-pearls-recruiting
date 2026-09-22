package com.company.hunttech.service;

import com.company.hunttech.core.ai.AiSecretService;
import com.company.hunttech.dto.yandex.*;
import com.company.hunttech.entity.CorporateYandexCalendar;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.UserYandexConfiguration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(YandexIntegrationService.NAME)
public class YandexIntegrationServiceBean implements YandexIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(YandexIntegrationServiceBean.class);

    private static final int HTTP_TIMEOUT_MS = 15000;
    private static final int HTTP_MULTI_STATUS = 207;
    private static final String DEFAULT_TIME_ZONE_SARATOV = "Europe/Saratov";

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private AiSecretService aiSecretService;
    @Inject
    private UserSessionSource userSessionSource;

    @Override
    public UserYandexConfiguration getOrCreateConfiguration(UUID userId) {
        if (userId == null) {
            userId = userSessionSource.getUserSession().getUser().getId();
        }
        UserYandexConfiguration config = dataManager.load(UserYandexConfiguration.class)
                .query("select e from hunttech_UserYandexConfiguration e where e.user.id = :userId")
                .parameter("userId", userId)
                .view("userYandexConfiguration-view")
                .optional()
                .orElse(null);

        if (config == null) {
            User user = dataManager.load(User.class).id(userId).view("_minimal").one();
            config = metadata.create(UserYandexConfiguration.class);
            config.setUser(user);
            if (user.getEmail() != null && user.getEmail().contains("@")) {
                config.setAccountEmail(user.getEmail());
            }
            config.setCalendarBaseUrl(UserYandexConfiguration.DEFAULT_CALENDAR_BASE_URL);
            config.setClientInterviewCalendarName(UserYandexConfiguration.DEFAULT_CLIENT_CALENDAR_NAME);
            config.setDefaultTimeZone(UserYandexConfiguration.DEFAULT_TIME_ZONE);
            config.setTelemostBaseUrl(UserYandexConfiguration.DEFAULT_TELEMOST_BASE_URL);
            config.setWikiBaseUrl(UserYandexConfiguration.DEFAULT_WIKI_BASE_URL);
            config.setWikiApiUrl(UserYandexConfiguration.DEFAULT_WIKI_API_URL);
            config.setWikiOrgId(UserYandexConfiguration.DEFAULT_WIKI_ORG_ID);
            config.setTelemostAutoRecord(true);
            config.setTelemostAiSummary(true);
            config = dataManager.commit(config);
        }
        return config;
    }

    @Override
    public UserYandexConfiguration saveConfiguration(UserYandexConfiguration config, String plainOauthToken, String plainRefreshToken) {
        if (StringUtils.isNotBlank(plainOauthToken)) {
            config.setOauthTokenEncrypted(aiSecretService.encrypt(plainOauthToken.trim()));
        }
        if (StringUtils.isNotBlank(plainRefreshToken)) {
            config.setRefreshTokenEncrypted(aiSecretService.encrypt(plainRefreshToken.trim()));
        }
        return dataManager.commit(config);
    }

    @Override
    public String encryptOauthToken(String plainOauthToken) {
        if (StringUtils.isBlank(plainOauthToken)) {
            return null;
        }
        return aiSecretService.encrypt(plainOauthToken.trim());
    }

    @Override
    public YandexDiagnosticResult testConnection(UUID userId, String serviceType) {
        UserYandexConfiguration config = getOrCreateConfiguration(userId);
        String token = resolveToken(config);
        if (StringUtils.isBlank(token)) {
            updateCalendarDiagnosticState(config, serviceType, false,
                    "Не указан OAuth токен авторизации Яндекс 360");
            return YandexDiagnosticResult.error(serviceType, 401, "Не указан OAuth токен авторизации Яндекс 360", null);
        }

        try {
            if ("CALENDAR".equalsIgnoreCase(serviceType)) {
                // Диагностика не использует fallback: синтетический путь календаря не доказывает
                // успешную OAuth-авторизацию или наличие прав CalDAV.
                List<YandexCalendarInfoDto> calendars = discoverCalendarsInternal(config, token, true);
                boolean foundClient = calendars.stream().anyMatch(YandexCalendarInfoDto::isClientInterviewCalendar);
                String msg = String.format("Календари доступны. Обнаружено %d календарей (в т.ч. 'Hunttech у заказчика': %s)",
                        calendars.size(), foundClient ? "найден" : "не найден, будет создан или использован личный");
                updateCalendarDiagnosticState(config, serviceType, true, msg);
                return YandexDiagnosticResult.ok("CALENDAR", msg);

            } else if ("TELEMOST".equalsIgnoreCase(serviceType)) {
                YandexTelemostConferenceDto conf = createTelemostInternal(config, token, true, true, "anyone");
                String msg = "Телемост API доступен. Сгенерирована ссылка: " + conf.getJoinUrl();
                config.setTelemostConnected(true);
                config.setLastVerifiedAt(new Date());
                config.setLastVerificationMessage(msg);
                dataManager.commit(config);
                return YandexDiagnosticResult.ok("TELEMOST", msg);

            } else if ("WIKI".equalsIgnoreCase(serviceType)) {
                String wikiUrl = StringUtils.defaultIfBlank(config.getWikiApiUrl(), UserYandexConfiguration.DEFAULT_WIKI_API_URL) + "/users/me";
                HttpResult res = sendHttp("GET", wikiUrl, token, null, "application/json", buildWikiHeaders(config));
                if (res.statusCode >= 200 && res.statusCode < 300) {
                    String msg = "Яндекс Вики API доступна (HTTP " + res.statusCode + ")";
                    config.setWikiConnected(true);
                    config.setLastVerifiedAt(new Date());
                    config.setLastVerificationMessage(msg);
                    dataManager.commit(config);
                    return YandexDiagnosticResult.ok("WIKI", msg);
                } else {
                    return YandexDiagnosticResult.error("WIKI", res.statusCode, "Ошибка Wiki API: HTTP " + res.statusCode, res.body);
                }

            } else {
                // Общая авторизация Яндекс (Passport / OAuth)
                HttpResult res = sendHttp("GET", "https://login.yandex.ru/info?format=json", token, null, "application/json", null);
                if (res.statusCode == 200) {
                    JSONObject json = new JSONObject(res.body);
                    String login = json.optString("login", "unknown");
                    String defaultEmail = json.optString("default_email", config.getAccountEmail());
                    String msg = String.format("Авторизация успешна. Пользователь: %s (%s)", login, defaultEmail);
                    return YandexDiagnosticResult.ok("AUTH", msg);
                } else {
                    return YandexDiagnosticResult.error("AUTH", res.statusCode, "Неверный токен авторизации", res.body);
                }
            }
        } catch (CalendarConnectionException ex) {
            String message = calendarFailureMessage(ex.getStatusCode());
            updateCalendarDiagnosticState(config, serviceType, false, message);
            log.warn("Диагностика календаря завершилась ошибкой: {}", ex.getMessage());
            return YandexDiagnosticResult.error(serviceType, ex.getStatusCode(), message, null);
        } catch (Exception ex) {
            updateCalendarDiagnosticState(config, serviceType, false, "Ошибка связи с календарным сервисом");
            log.error("Ошибка при проверке подключения к {}: {}", serviceType, ex.getMessage(), ex);
            return YandexDiagnosticResult.error(serviceType, 500, "Ошибка связи: " + ex.getMessage(), null);
        }
    }

    /**
     * Сохраняет результат календарной диагностики без токенов и ответов внешнего сервиса.
     */
    protected void updateCalendarDiagnosticState(UserYandexConfiguration config, String serviceType,
                                                 boolean connected, String message) {
        if (config == null || !"CALENDAR".equalsIgnoreCase(serviceType)) {
            return;
        }
        config.setCalendarConnected(connected);
        config.setLastVerifiedAt(new Date());
        config.setLastVerificationMessage(message);
        dataManager.commit(config);
    }

    private String calendarFailureMessage(int statusCode) {
        if (statusCode == 401) {
            return "OAuth-токен Яндекс недействителен или истёк";
        }
        if (statusCode == 403) {
            return "Недостаточно прав OAuth для доступа к календарям";
        }
        if (statusCode == 404) {
            return "CalDAV-календарь или учётная запись недоступны";
        }
        if (statusCode == 503) {
            return "Сетевая ошибка доступа к Яндекс.Календарю";
        }
        if (statusCode == 502) {
            return "Сервис Яндекс.Календаря вернул некорректный ответ";
        }
        return "Ошибка CalDAV: HTTP " + statusCode;
    }
    @Override
    public List<YandexCalendarInfoDto> discoverCalendars(UUID userId) {
        UserYandexConfiguration config = getOrCreateConfiguration(userId);
        String token = resolveToken(config);
        if (StringUtils.isBlank(token)) {
            throw new RuntimeException("OAuth-токен не настроен");
        }
        return discoverCalendarsInternal(config, token);
    }

    @Override
    public YandexTelemostConferenceDto createTelemostConference(UUID userId, boolean autoRecord, boolean aiSummary, String accessLevel) {
        UserYandexConfiguration config = getOrCreateConfiguration(userId);
        String token = resolveToken(config);
        if (StringUtils.isBlank(token)) {
            throw new RuntimeException("OAuth-токен не настроен для пользователя");
        }
        try {
            return createTelemostInternal(config, token, autoRecord, aiSummary, accessLevel);
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка вызова Telemost API: " + ex.getMessage(), ex);
        }
    }

    @Override
    public YandexMeetingResult scheduleCalendarEvent(UUID userId, YandexMeetingRequest request) {
        UserYandexConfiguration config = getOrCreateConfiguration(userId);
        String token = resolveToken(config);
        String effectiveAccountEmail = null;
        CorporateYandexCalendar matchingCorpCal = null;

        String customPath = request.getCustomCalendarPath();
        if (StringUtils.isNotBlank(customPath)) {
            matchingCorpCal = resolveCorporateCalendarForPath(customPath);
        } else if (request.getCalendarType() == YandexCalendarType.CLIENT_INTERVIEW && StringUtils.isBlank(config.getClientInterviewCalendarPath())) {
            // Для собеседований с заказчиками при отсутствии личного пути разрешаем дефолтный корпоративный календарь
            matchingCorpCal = resolveCorporateCalendarForPath(null);
        }

        // Если календарь корпоративный — используем его токен и учетные данные
        if (matchingCorpCal != null) {
            if (StringUtils.isNotBlank(matchingCorpCal.getOauthTokenEncrypted())) {
                try {
                    token = aiSecretService.decrypt(matchingCorpCal.getOauthTokenEncrypted());
                    effectiveAccountEmail = StringUtils.trimToNull(matchingCorpCal.getAccountEmail());
                    if (StringUtils.isBlank(effectiveAccountEmail)) {
                        effectiveAccountEmail = fetchAccountEmailForToken(token);
                    }
                } catch (Exception e) {
                    log.error("Не удалось расшифровать токен корпоративного календаря {}: {}", matchingCorpCal.getName(), e.getMessage(), e);
                    return YandexMeetingResult.error("Не удалось расшифровать ключ корпоративного календаря '" + matchingCorpCal.getName() + "': " + e.getMessage());
                }
            } else if (StringUtils.isNotBlank(token)) {
                log.warn("У корпоративного календаря '{}' нет собственного токена, используется токен пользователя", matchingCorpCal.getName());
            } else {
                return YandexMeetingResult.error("OAuth токен для корпоративного календаря '" + matchingCorpCal.getName() + "' не настроен");
            }
        }
        if (StringUtils.isBlank(token)) {
            return YandexMeetingResult.error("OAuth токен Яндекс не настроен");
        }

        try {
            // 1. Если требуется Телемост — генерируем конференцию
            String telemostJoinUrl = null;
            String telemostConfId = null;
            if (request.isCreateTelemostMeeting()) {
                try {
                    YandexTelemostConferenceDto telemost = createTelemostInternal(
                            config, token, request.isTelemostAutoRecord(), request.isTelemostAiSummary(), "anyone");
                    telemostJoinUrl = telemost.getJoinUrl();
                    telemostConfId = telemost.getId();
                    log.info("Создана конференция Телемост: {}", telemostJoinUrl);
                } catch (Exception ex) {
                    log.warn("Не удалось создать ссылку Телемоста: {}. Продолжаем без нее.", ex.getMessage());
                }
            }

            // 2. Определяем целевой календарь
            String targetCalendarPath = matchingCorpCal != null
                    ? matchingCorpCal.getCalendarPath()
                    : resolveTargetCalendarPath(config, token, request.getCalendarType(), request.getCustomCalendarPath());
            String calendarDisplayName = request.getCalendarType() == YandexCalendarType.CLIENT_INTERVIEW
                    ? config.getClientInterviewCalendarName()
                    : StringUtils.defaultIfBlank(config.getPersonalCalendarName(), DEFAULT_PERSONAL_CALENDAR_NAME);
            if (matchingCorpCal != null) {
                calendarDisplayName = matchingCorpCal.getName();
            }

            // 3. Формируем UID и iCalendar
            String eventUid = StringUtils.isNotBlank(request.getEventUid())
                    ? request.getEventUid().trim()
                    : UUID.randomUUID().toString();
            String timeZone = StringUtils.defaultIfBlank(request.getTimeZone(),
                    StringUtils.defaultIfBlank(config.getDefaultTimeZone(), DEFAULT_TIME_ZONE_SARATOV));

            String icsBody = buildIcsContent(eventUid, request, config, effectiveAccountEmail, telemostJoinUrl, timeZone);

            // 4. Запись события в CalDAV через PUT
            String baseUrl = resolveBaseUrl(matchingCorpCal, config);
            String eventUrl = normalizeUrl(baseUrl, targetCalendarPath) + "/" + eventUid + ".ics";

            log.info("Отправка CalDAV PUT: eventUrl={}", eventUrl);
            HttpResult putResult = sendHttp("PUT", eventUrl, token, icsBody, "text/calendar; charset=utf-8", null);
            if (putResult.statusCode != 201 && putResult.statusCode != 204 && putResult.statusCode != 200) {
                return YandexMeetingResult.error("Ошибка сохранения в CalDAV: HTTP " + putResult.statusCode + " " + putResult.body);
            }

            // 5. Отправка инвайтов через CalDAV Outbox
            List<String> invited = new ArrayList<>(request.getAttendeeEmails());
            if (StringUtils.isNotBlank(request.getCandidateEmail()) && !invited.contains(request.getCandidateEmail())) {
                invited.add(request.getCandidateEmail());
            }

            if (!invited.isEmpty()) {
                String account = StringUtils.isNotBlank(effectiveAccountEmail)
                        ? effectiveAccountEmail
                        : (matchingCorpCal != null
                                ? resolveCorporateAccountEmail(matchingCorpCal, token)
                                : resolveAccountEmail(config, token));
                if (StringUtils.isNotBlank(account) && !"user@yandex.ru".equalsIgnoreCase(account)) {
                    String outboxUrl = normalizeUrl(baseUrl, "/calendars/" + URLEncoder.encode(account, StandardCharsets.UTF_8.name()) + "/outbox/");
                    Map<String, String> outboxHeaders = new HashMap<>();
                    outboxHeaders.put("Origin", "https://calendar.yandex.ru");
                    try {
                        HttpResult outboxResult = sendHttp("POST", outboxUrl, token, icsBody, "text/calendar; charset=utf-8", outboxHeaders);
                        log.info("CalDAV outbox результат: HTTP {}", outboxResult.statusCode);
                    } catch (Exception ex) {
                        log.warn("Ошибка отправки инвайтов в outbox: {}", ex.getMessage());
                    }
                } else {
                    log.warn("Email учетной записи не определен, пропуск отправки через CalDAV outbox");
                }
            }

            // 6. Read-back проверка
            HttpResult getResult = sendHttp("GET", eventUrl, token, null, "text/calendar", null);
            if (getResult.statusCode != 200) {
                log.warn("Read-back проверка события вернула HTTP {}", getResult.statusCode);
            }

            YandexMeetingResult result = YandexMeetingResult.success(eventUid, calendarDisplayName, telemostJoinUrl,
                    "Встреча успешно создана в календаре '" + calendarDisplayName + "'");
            result.setCalendarPath(targetCalendarPath);
            result.setTelemostConferenceId(telemostConfId);
            result.setSummary(request.getTitle());
            result.setStartTime(request.getStartTime());
            result.setEndTime(request.getEndTime());
            result.setInvitedEmails(invited);
            result.setIcsContent(icsBody);

            return result;
        } catch (Exception ex) {
            log.error("Исключение при планировании встречи: {}", ex.getMessage(), ex);
            return YandexMeetingResult.error("Ошибка при планировании: " + ex.getMessage());
        }
    }

    @Override
    public boolean cancelCalendarEvent(UUID userId, String calendarPath, String eventUid) {
        if (StringUtils.isBlank(calendarPath) || StringUtils.isBlank(eventUid)) {
            return false;
        }
        UserYandexConfiguration config = getOrCreateConfiguration(userId);
        String token = resolveToken(config);
        CorporateYandexCalendar matchingCorpCal = resolveCorporateCalendarForPath(calendarPath);
        if (matchingCorpCal != null && StringUtils.isNotBlank(matchingCorpCal.getOauthTokenEncrypted())) {
            try {
                token = aiSecretService.decrypt(matchingCorpCal.getOauthTokenEncrypted());
            } catch (Exception e) {
                log.error("Не удалось расшифровать токен корпоративного календаря {}: {}", matchingCorpCal.getName(), e.getMessage(), e);
                return false;
            }
        }
        if (StringUtils.isBlank(token)) {
            return false;
        }
        try {
            String baseUrl = resolveBaseUrl(matchingCorpCal, config);
            String eventUrl = normalizeUrl(baseUrl, calendarPath) + "/" + eventUid + ".ics";
            HttpResult res = sendHttp("DELETE", eventUrl, token, null, null, null);
            return res.statusCode == 200 || res.statusCode == 204;
        } catch (Exception ex) {
            log.error("Ошибка удаления события {}: {}", eventUid, ex.getMessage(), ex);
            return false;
        }
    }

    private String resolveBaseUrl(CorporateYandexCalendar corpCal, UserYandexConfiguration config) {
        if (corpCal != null && StringUtils.isNotBlank(corpCal.getCalendarBaseUrl())) {
            return corpCal.getCalendarBaseUrl().trim();
        }
        return config != null && StringUtils.isNotBlank(config.getCalendarBaseUrl())
                ? config.getCalendarBaseUrl().trim()
                : UserYandexConfiguration.DEFAULT_CALENDAR_BASE_URL;
    }

    @Override
    public List<YandexCalendarEventDto> getCalendarEvents(UUID userId, String calendarPath, Date from, Date to) {
        UserYandexConfiguration config = getOrCreateConfiguration(userId);
        String token = resolveToken(config);
        if (StringUtils.isBlank(token)) {
            log.warn("OAuth-токен Яндекс не настроен для пользователя {}", userId);
            return Collections.emptyList();
        }

        if (StringUtils.isBlank(calendarPath)) {
            calendarPath = resolveTargetCalendarPath(config, token, YandexCalendarType.PERSONAL, null);
        }

        String tzStr = StringUtils.defaultIfBlank(config.getDefaultTimeZone(), DEFAULT_TIME_ZONE_SARATOV);
        TimeZone timeZone = TimeZone.getTimeZone(tzStr);

        Date effectiveFrom = from != null ? from : new Date(System.currentTimeMillis() - 3600000L);
        Date effectiveTo = to != null ? to : new Date(System.currentTimeMillis() + 14L * 24 * 3600 * 1000);

        String calendarDisplayName = resolveCalendarDisplayName(config, calendarPath);
        return fetchEventsFromCaldav(config, token, calendarPath, calendarDisplayName, effectiveFrom, effectiveTo, timeZone);
    }

    @Override
    public List<YandexCalendarEventDto> getAllUpcomingCalendarEvents(UUID userId, Date from, Date to) {
        UserYandexConfiguration config = getOrCreateConfiguration(userId);
        String token = resolveToken(config);
        if (StringUtils.isBlank(token)) {
            return Collections.emptyList();
        }

        String tzStr = StringUtils.defaultIfBlank(config.getDefaultTimeZone(), DEFAULT_TIME_ZONE_SARATOV);
        TimeZone timeZone = TimeZone.getTimeZone(tzStr);

        Date effectiveFrom = from != null ? from : new Date(System.currentTimeMillis() - 3600000L);
        Date effectiveTo = to != null ? to : new Date(System.currentTimeMillis() + 14L * 24 * 3600 * 1000);

        List<YandexCalendarEventDto> allEvents = new ArrayList<>();
        Set<String> seenUids = new HashSet<>();

        // 1. Личный календарь
        String personalPath = StringUtils.isNotBlank(config.getPersonalCalendarPath())
                ? config.getPersonalCalendarPath()
                : resolveTargetCalendarPath(config, token, YandexCalendarType.PERSONAL, null);
        String personalName = StringUtils.defaultIfBlank(config.getPersonalCalendarName(), DEFAULT_PERSONAL_CALENDAR_NAME);
        if (StringUtils.isNotBlank(personalPath)) {
            List<YandexCalendarEventDto> personalEvents = fetchEventsFromCaldav(config, token, personalPath, personalName, effectiveFrom, effectiveTo, timeZone);
            for (YandexCalendarEventDto ev : personalEvents) {
                if (ev.getUid() == null || seenUids.add(ev.getUid())) {
                    ev.setCalendarName(personalName);
                    allEvents.add(ev);
                }
            }
        }

        // 2. Календарь собеседований с заказчиком («Hunttech у заказчика»)
        String clientPath = StringUtils.isNotBlank(config.getClientInterviewCalendarPath())
                ? config.getClientInterviewCalendarPath()
                : resolveTargetCalendarPath(config, token, YandexCalendarType.CLIENT_INTERVIEW, null);
        String clientName = StringUtils.defaultIfBlank(config.getClientInterviewCalendarName(), UserYandexConfiguration.DEFAULT_CLIENT_CALENDAR_NAME);
        if (StringUtils.isNotBlank(clientPath) && !clientPath.equalsIgnoreCase(personalPath)) {
            List<YandexCalendarEventDto> clientEvents = fetchEventsFromCaldav(config, token, clientPath, clientName, effectiveFrom, effectiveTo, timeZone);
            for (YandexCalendarEventDto ev : clientEvents) {
                if (ev.getUid() == null || seenUids.add(ev.getUid())) {
                    ev.setCalendarName(clientName);
                    allEvents.add(ev);
                }
            }
        }

        Collections.sort(allEvents);
        return allEvents;
    }

    private List<YandexCalendarEventDto> fetchEventsFromCaldav(UserYandexConfiguration config, String token,
                                                               String calendarPath, String calendarDisplayName,
                                                               Date from, Date to, TimeZone timeZone) {
        List<YandexCalendarEventDto> events = new ArrayList<>();
        String baseUrl = StringUtils.defaultIfBlank(config.getCalendarBaseUrl(), UserYandexConfiguration.DEFAULT_CALENDAR_BASE_URL);
        String url = normalizeUrl(baseUrl, calendarPath);

        SimpleDateFormat utcFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String startUtc = utcFormat.format(from);
        String endUtc = utcFormat.format(to);

        // 1. CalDAV REPORT с calendar-query time-range
        String reportXml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<c:calendar-query xmlns:d=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\">\n" +
                "  <d:prop>\n" +
                "    <d:getetag />\n" +
                "    <c:calendar-data />\n" +
                "  </d:prop>\n" +
                "  <c:filter>\n" +
                "    <c:comp-filter name=\"VCALENDAR\">\n" +
                "      <c:comp-filter name=\"VEVENT\">\n" +
                "        <c:time-range start=\"" + startUtc + "\" end=\"" + endUtc + "\"/>\n" +
                "      </c:comp-filter>\n" +
                "    </c:comp-filter>\n" +
                "  </c:filter>\n" +
                "</c:calendar-query>";

        Map<String, String> headers = new HashMap<>();
        headers.put("Depth", "1");
        headers.put("Prefer", "return-minimal");

        try {
            HttpResult res = sendHttp("REPORT", url, token, reportXml, "application/xml; charset=utf-8", headers);
            if (res.statusCode == HTTP_MULTI_STATUS || res.statusCode == 200) {
                events = parseCaldavResponseXml(res.body, calendarDisplayName, calendarPath, timeZone);
            } else {
                log.warn("CalDAV REPORT для {} вернул HTTP {}, переключаемся на PROPFIND", url, res.statusCode);
                // Фолбэк: PROPFIND с запросом calendar-data
                String propfindXml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                        "<d:propfind xmlns:d=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\">\n" +
                        "  <d:prop>\n" +
                        "    <d:displayname />\n" +
                        "    <c:calendar-data />\n" +
                        "  </d:prop>\n" +
                        "</d:propfind>";
                HttpResult propRes = sendHttp("PROPFIND", url, token, propfindXml, "application/xml; charset=utf-8", Collections.singletonMap("Depth", "1"));
                if (propRes.statusCode == HTTP_MULTI_STATUS || propRes.statusCode == 200) {
                    events = parseCaldavResponseXml(propRes.body, calendarDisplayName, calendarPath, timeZone);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка запроса событий CalDAV из {}: {}", url, e.getMessage(), e);
        }

        // Фильтруем события по пересечению с [from, to]
        List<YandexCalendarEventDto> filtered = new ArrayList<>();
        for (YandexCalendarEventDto ev : events) {
            if (ev.getStartTime() != null) {
                Date evEnd = ev.getEndTime() != null ? ev.getEndTime() : new Date(ev.getStartTime().getTime() + 3600000L);
                if (evEnd.compareTo(from) >= 0 && ev.getStartTime().compareTo(to) <= 0) {
                    filtered.add(ev);
                }
            }
        }
        Collections.sort(filtered);
        return filtered;
    }

    public static List<YandexCalendarEventDto> parseCaldavResponseXml(String xmlBody, String calendarDisplayName, String calendarPath, TimeZone defaultTz) {
        List<YandexCalendarEventDto> result = new ArrayList<>();
        if (StringUtils.isBlank(xmlBody)) {
            return result;
        }

        Pattern calDataPattern = Pattern.compile("<(?:\\w+:)?calendar-data[^>]*>(.*?)</(?:\\w+:)?calendar-data>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = calDataPattern.matcher(xmlBody);

        while (matcher.find()) {
            String icsRaw = matcher.group(1);
            icsRaw = unescapeXmlEntities(icsRaw);
            List<YandexCalendarEventDto> parsedEvents = parseIcsEvents(icsRaw, calendarDisplayName, calendarPath, defaultTz);
            result.addAll(parsedEvents);
        }

        return result;
    }

    private static String unescapeXmlEntities(String text) {
        if (text == null) return "";
        return text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    public static List<YandexCalendarEventDto> parseIcsEvents(String icsContent, String calendarDisplayName, String calendarPath, TimeZone defaultTz) {
        List<YandexCalendarEventDto> events = new ArrayList<>();
        if (StringUtils.isBlank(icsContent)) {
            return events;
        }

        // Unfolding по стандарту RFC 5545
        String unfolded = icsContent.replaceAll("\r?\n[ \t]", "");

        Pattern eventPattern = Pattern.compile("BEGIN:VEVENT(.*?)END:VEVENT", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = eventPattern.matcher(unfolded);

        while (matcher.find()) {
            String eventBlock = matcher.group(1);
            YandexCalendarEventDto dto = parseSingleVEvent(eventBlock, calendarDisplayName, calendarPath, defaultTz);
            if (dto != null && dto.getStartTime() != null) {
                events.add(dto);
            }
        }
        return events;
    }

    private static YandexCalendarEventDto parseSingleVEvent(String block, String calendarName, String calendarPath, TimeZone defaultTz) {
        YandexCalendarEventDto dto = new YandexCalendarEventDto();
        dto.setCalendarName(calendarName);
        dto.setCalendarPath(calendarPath);
        dto.setTimeZone(defaultTz != null ? defaultTz.getID() : DEFAULT_TIME_ZONE_SARATOV);
        dto.setRawIcs(block);

        String[] lines = block.split("\r?\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            int colonIdx = line.indexOf(':');
            if (colonIdx <= 0) continue;

            String propPart = line.substring(0, colonIdx);
            String valPart = line.substring(colonIdx + 1);

            String propName = propPart.split(";")[0].toUpperCase();

            switch (propName) {
                case "UID":
                    dto.setUid(valPart.trim());
                    break;
                case "SUMMARY":
                    dto.setSummary(unescapeIcsValue(valPart));
                    break;
                case "DESCRIPTION":
                    dto.setDescription(unescapeIcsValue(valPart));
                    break;
                case "LOCATION":
                    dto.setLocation(unescapeIcsValue(valPart));
                    break;
                case "STATUS":
                    dto.setStatus(valPart.trim());
                    break;
                case "DTSTART":
                    parseEventDate(propPart, valPart, defaultTz, dto, true);
                    break;
                case "DTEND":
                    parseEventDate(propPart, valPart, defaultTz, dto, false);
                    break;
                case "ATTENDEE":
                    String email = extractEmailFromAttendee(valPart);
                    if (StringUtils.isNotBlank(email)) {
                        dto.getAttendees().add(email);
                    }
                    break;
                case "ORGANIZER":
                    dto.setOrganizer(extractEmailFromAttendee(valPart));
                    break;
                case "URL":
                case "X-TELEMOST-URL":
                    if (valPart.contains("telemost.yandex.ru")) {
                        dto.setTelemostUrl(valPart.trim());
                    }
                    break;
            }
        }

        // Если telemostUrl не найден в свойстве, проверяем location и description
        if (StringUtils.isBlank(dto.getTelemostUrl())) {
            String telemostRegex = "https://telemost\\.yandex\\.ru/j/[a-zA-Z0-9_-]+";
            Pattern tp = Pattern.compile(telemostRegex);
            if (StringUtils.isNotBlank(dto.getLocation())) {
                Matcher tm = tp.matcher(dto.getLocation());
                if (tm.find()) dto.setTelemostUrl(tm.group(0));
            }
            if (StringUtils.isBlank(dto.getTelemostUrl()) && StringUtils.isNotBlank(dto.getDescription())) {
                Matcher tm = tp.matcher(dto.getDescription());
                if (tm.find()) dto.setTelemostUrl(tm.group(0));
            }
        }

        if (dto.getEndTime() == null && dto.getStartTime() != null) {
            dto.setEndTime(new Date(dto.getStartTime().getTime() + (dto.isAllDay() ? 24 * 3600 * 1000L : 3600000L)));
        }

        return dto;
    }

    private static void parseEventDate(String propPart, String valPart, TimeZone defaultTz, YandexCalendarEventDto dto, boolean isStart) {
        valPart = valPart.trim();
        TimeZone tz = defaultTz;

        if (propPart.contains("TZID=")) {
            int tzidIdx = propPart.indexOf("TZID=");
            String tzid = propPart.substring(tzidIdx + 5).split(";")[0].replace("\"", "").trim();
            tz = TimeZone.getTimeZone(tzid);
        }

        try {
            if (valPart.endsWith("Z")) {
                // UTC формат
                SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
                fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = fmt.parse(valPart);
                if (isStart) dto.setStartTime(d); else dto.setEndTime(d);
            } else if (valPart.contains("T")) {
                // Локальное время с часами
                SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
                fmt.setTimeZone(tz != null ? tz : TimeZone.getDefault());
                Date d = fmt.parse(valPart);
                if (isStart) dto.setStartTime(d); else dto.setEndTime(d);
            } else if (valPart.length() == 8) {
                // Весь день: yyyyMMdd
                SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
                fmt.setTimeZone(tz != null ? tz : TimeZone.getDefault());
                Date d = fmt.parse(valPart);
                if (isStart) {
                    dto.setStartTime(d);
                    dto.setAllDay(true);
                } else {
                    dto.setEndTime(d);
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось распарсить дату iCal: {} ({})", valPart, propPart);
        }
    }

    private static String extractEmailFromAttendee(String val) {
        if (StringUtils.isBlank(val)) return null;
        if (val.toLowerCase().startsWith("mailto:")) {
            return val.substring(7).trim();
        }
        int mailtoIdx = val.toLowerCase().indexOf("mailto:");
        if (mailtoIdx >= 0) {
            return val.substring(mailtoIdx + 7).trim();
        }
        if (val.contains("@")) {
            return val.trim();
        }
        return null;
    }

    private static String unescapeIcsValue(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (next == 'n' || next == 'N') {
                    sb.append('\n');
                    i++;
                } else if (next == ',' || next == ';' || next == '\\') {
                    sb.append(next);
                    i++;
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private String resolveCalendarDisplayName(UserYandexConfiguration config, String calendarPath) {
        if (calendarPath == null) return "Календарь";
        if (calendarPath.equalsIgnoreCase(config.getClientInterviewCalendarPath())) {
            return StringUtils.defaultIfBlank(config.getClientInterviewCalendarName(), UserYandexConfiguration.DEFAULT_CLIENT_CALENDAR_NAME);
        }
        if (calendarPath.equalsIgnoreCase(config.getPersonalCalendarPath())) {
            return StringUtils.defaultIfBlank(config.getPersonalCalendarName(), DEFAULT_PERSONAL_CALENDAR_NAME);
        }
        return "Яндекс-Календарь";
    }

    // --- Внутренние вспомогательные методы ---

    protected String resolveToken(UserYandexConfiguration config) {
        if (config == null || StringUtils.isBlank(config.getOauthTokenEncrypted())) {
            return null;
        }
        try {
            return aiSecretService.decrypt(config.getOauthTokenEncrypted());
        } catch (Exception e) {
            log.error("Не удалось расшифровать OAuth токен: {}", e.getMessage());
            return null;
        }
    }

    private String fetchAccountEmailForToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        try {
            HttpResult res = sendHttp("GET", "https://login.yandex.ru/info?format=json", token, null, "application/json", null);
            if (res.statusCode == 200) {
                JSONObject json = new JSONObject(res.body);
                String email = json.optString("default_email", json.optString("login", null));
                if (StringUtils.isNotBlank(email)) {
                    return email.trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String resolveCorporateAccountEmail(CorporateYandexCalendar corpCal, String token) {
        if (corpCal != null && StringUtils.isNotBlank(corpCal.getAccountEmail())) {
            return corpCal.getAccountEmail().trim();
        }
        String fetched = fetchAccountEmailForToken(token);
        if (corpCal != null && StringUtils.isNotBlank(fetched)) {
            corpCal.setAccountEmail(fetched);
            dataManager.commit(corpCal);
            return fetched;
        }
        return StringUtils.isNotBlank(fetched) ? fetched : "user@yandex.ru";
    }

    private String resolveAccountEmail(UserYandexConfiguration config, String token) {
        if (config != null && StringUtils.isNotBlank(config.getAccountEmail())) {
            return config.getAccountEmail().trim();
        }
        String fetched = fetchAccountEmailForToken(token);
        if (config != null && StringUtils.isNotBlank(fetched)) {
            config.setAccountEmail(fetched);
            dataManager.commit(config);
            return fetched;
        }
        return StringUtils.isNotBlank(fetched) ? fetched : "user@yandex.ru";
    }

    private List<YandexCalendarInfoDto> discoverCalendarsInternal(UserYandexConfiguration config, String token) {
        return discoverCalendarsInternal(config, token, false);
    }

    /**
     * Выполняет CalDAV discovery. Строгий режим предназначен только для диагностики подключения:
     * он запрещает маскировать HTTP- и сетевые ошибки синтетическими fallback-календарями.
     */
    protected List<YandexCalendarInfoDto> discoverCalendarsInternal(UserYandexConfiguration config, String token,
                                                                    boolean strictDiagnostics) {
        List<YandexCalendarInfoDto> result = new ArrayList<>();
        String baseUrl = StringUtils.defaultIfBlank(config.getCalendarBaseUrl(), UserYandexConfiguration.DEFAULT_CALENDAR_BASE_URL);
        String account = resolveAccountEmail(config, token);

        try {
            // 1. PROPFIND principal
            String principalUrl = normalizeUrl(baseUrl, "/principals/users/" + URLEncoder.encode(account, StandardCharsets.UTF_8.name()) + "/");
            String propfindBody = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                    "<d:propfind xmlns:d=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\">\n" +
                    "  <d:prop>\n" +
                    "    <c:calendar-home-set />\n" +
                    "  </d:prop>\n" +
                    "</d:propfind>";

            HttpResult propfindRes = sendHttp("PROPFIND", principalUrl, token, propfindBody, "application/xml; charset=utf-8", Collections.singletonMap("Depth", "0"));
            requireSuccessfulCaldavResponse(propfindRes, "получение principal", strictDiagnostics);

            String homeSetPath = extractTagContent(propfindRes.body, "href");
            if (StringUtils.isBlank(homeSetPath)) {
                homeSetPath = "/calendars/" + URLEncoder.encode(account, StandardCharsets.UTF_8.name()) + "/";
            }

            // 2. PROPFIND Depth 1 на homeSetPath
            String homeUrl = normalizeUrl(baseUrl, homeSetPath);
            String listBody = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                    "<d:propfind xmlns:d=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\">\n" +
                    "  <d:prop>\n" +
                    "    <d:displayname />\n" +
                    "    <d:resourcetype />\n" +
                    "  </d:prop>\n" +
                    "</d:propfind>";

            HttpResult listRes = sendHttp("PROPFIND", homeUrl, token, listBody, "application/xml; charset=utf-8", Collections.singletonMap("Depth", "1"));
            requireSuccessfulCaldavResponse(listRes, "получение списка календарей", strictDiagnostics);

            // Разбираем ответы
            Pattern responsePattern = Pattern.compile("<(?:\\w+:)?response>(.*?)</(?:\\w+:)?response>", Pattern.DOTALL);
            Matcher matcher = responsePattern.matcher(listRes.body);

            while (matcher.find()) {
                String responseXml = matcher.group(1);
                boolean hasResourceType = Pattern.compile("<(?:\\w+:)?resourcetype", Pattern.CASE_INSENSITIVE)
                        .matcher(responseXml)
                        .find();
                if (hasResourceType && (responseXml.contains("calendar") || responseXml.contains("collection"))) {
                    String href = extractTagContent(responseXml, "href");
                    String displayname = extractTagContent(responseXml, "displayname");
                    if (StringUtils.isNotBlank(href) && !href.equals(homeSetPath)) {
                        boolean isClient = displayname != null && displayname.equalsIgnoreCase(UserYandexConfiguration.DEFAULT_CLIENT_CALENDAR_NAME);
                        boolean isDef = result.isEmpty();
                        result.add(new YandexCalendarInfoDto(href, displayname != null ? displayname : href, href, isDef, isClient));
                    }
                }
            }

            if (result.isEmpty()) {
                if (strictDiagnostics) {
                    throw new CalendarConnectionException(404,
                            "CalDAV не вернул доступных календарей");
                }
                // Фолбэк на стандартный путь календаря пользователя
                String defaultPath = "/calendars/" + URLEncoder.encode(account, StandardCharsets.UTF_8.name()) + "/events/";
                result.add(new YandexCalendarInfoDto("default", "Основной", defaultPath, true, false));
                String clientPath = "/calendars/" + URLEncoder.encode(account, StandardCharsets.UTF_8.name()) + "/hunttech-client/";
                result.add(new YandexCalendarInfoDto("client", UserYandexConfiguration.DEFAULT_CLIENT_CALENDAR_NAME, clientPath, false, true));
            }

        } catch (Exception ex) {
            if (strictDiagnostics) {
                if (ex instanceof CalendarConnectionException) {
                    throw (CalendarConnectionException) ex;
                }
                log.error("Не удалось выполнить CalDAV discovery при диагностике подключения", ex);
                if (ex instanceof IOException) {
                    throw new CalendarConnectionException(503, "сетевая ошибка CalDAV");
                }
                throw new CalendarConnectionException(502, "некорректный ответ CalDAV");
            }
            log.warn("Ошибка CalDAV discovery: {}", ex.getMessage());
            // Фолбэк
            String defaultPath = "/calendars/" + account + "/events/";
            result.add(new YandexCalendarInfoDto("default", DEFAULT_PERSONAL_CALENDAR_NAME, defaultPath, true, false));
        }

        return result;
    }

    private void requireSuccessfulCaldavResponse(HttpResult response, String operation, boolean strictDiagnostics) {
        if (!strictDiagnostics || (response.statusCode >= 200 && response.statusCode < 300)) {
            return;
        }
        throw new CalendarConnectionException(response.statusCode,
                operation + " завершено с HTTP " + response.statusCode);
    }

    private String resolveTargetCalendarPath(UserYandexConfiguration config, String token, YandexCalendarType calendarType, String customPath) {
        if (StringUtils.isNotBlank(customPath)) {
            return customPath;
        }
        if (calendarType == YandexCalendarType.CLIENT_INTERVIEW) {
            if (StringUtils.isNotBlank(config.getClientInterviewCalendarPath())) {
                return config.getClientInterviewCalendarPath();
            }
            List<YandexCalendarInfoDto> calendars = discoverCalendarsInternal(config, token);
            for (YandexCalendarInfoDto c : calendars) {
                if (c.isClientInterviewCalendar() || (c.getDisplayName() != null && c.getDisplayName().equalsIgnoreCase(UserYandexConfiguration.DEFAULT_CLIENT_CALENDAR_NAME))) {
                    config.setClientInterviewCalendarPath(c.getPath());
                    dataManager.commit(config);
                    return c.getPath();
                }
            }
            // Корпоративный календарь собеседований с заказчиком не найден среди существующих —
            // формируем детерминированный путь для календаря заказчика
            try {
                String account = resolveAccountEmail(config, token);
                String clientPath = "/calendars/" + URLEncoder.encode(account, StandardCharsets.UTF_8.name()) + "/hunttech-client/";
                config.setClientInterviewCalendarPath(clientPath);
                dataManager.commit(config);
                return clientPath;
            } catch (Exception e) {
                log.warn("Не удалось вычислить путь календаря заказчика: {}", e.getMessage());
            }
        } else {
            if (StringUtils.isNotBlank(config.getPersonalCalendarPath())) {
                return config.getPersonalCalendarPath();
            }
        }

        List<YandexCalendarInfoDto> calendars = discoverCalendarsInternal(config, token);
        if (!calendars.isEmpty()) {
            return calendars.get(0).getPath();
        }
        String account = resolveAccountEmail(config, token);
        return "/calendars/" + account + "/events/";
    }

    private YandexTelemostConferenceDto createTelemostInternal(UserYandexConfiguration config, String token, boolean autoRecord, boolean aiSummary, String accessLevel) throws Exception {
        String baseUrl = StringUtils.defaultIfBlank(config.getTelemostBaseUrl(), UserYandexConfiguration.DEFAULT_TELEMOST_BASE_URL);
        String url = baseUrl + "/conferences";

        JSONObject requestJson = new JSONObject();
        requestJson.put("access_level", StringUtils.defaultIfBlank(accessLevel, "anyone"));

        HttpResult res = sendHttp("POST", url, token, requestJson.toString(), "application/json", null);
        if (res.statusCode >= 200 && res.statusCode < 300) {
            JSONObject json = new JSONObject(res.body);
            String id = json.optString("id", UUID.randomUUID().toString());
            String joinUrl = json.optString("join_url");
            String invitation = json.optString("invitation", "Присоединиться к видеовстрече Телемост: " + joinUrl);
            String level = json.optString("access_level", accessLevel);
            return new YandexTelemostConferenceDto(id, joinUrl, invitation, level);
        } else {
            throw new RuntimeException("Telemost API HTTP " + res.statusCode + ": " + res.body);
        }
    }

    protected CorporateYandexCalendar resolveCorporateCalendarForPath(String calendarPath) {
        if (StringUtils.isNotBlank(calendarPath)) {
            List<CorporateYandexCalendar> list = dataManager.load(CorporateYandexCalendar.class)
                    .query("select e from hunttech_CorporateYandexCalendar e where e.calendarPath = :path and e.active = true")
                    .parameter("path", calendarPath.trim())
                    .maxResults(1)
                    .list();
            if (!list.isEmpty()) {
                return list.get(0);
            }
            return null;
        }
        List<CorporateYandexCalendar> defaultList = dataManager.load(CorporateYandexCalendar.class)
                .query("select e from hunttech_CorporateYandexCalendar e where e.isDefault = true and e.active = true")
                .maxResults(1)
                .list();
        return defaultList.isEmpty() ? null : defaultList.get(0);
    }

    private String buildIcsContent(String uid, YandexMeetingRequest req, UserYandexConfiguration config, String effectiveAccountEmail, String telemostUrl, String timeZone) {
        SimpleDateFormat utcFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat localFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        localFormat.setTimeZone(TimeZone.getTimeZone(timeZone));

        String nowUtc = utcFormat.format(new Date());
        String startStr = localFormat.format(req.getStartTime() != null ? req.getStartTime() : new Date());
        String endStr = localFormat.format(req.getEndTime() != null ? req.getEndTime() : new Date(System.currentTimeMillis() + 3600000));

        String organizerEmail = StringUtils.isNotBlank(effectiveAccountEmail)
                ? effectiveAccountEmail
                : resolveAccountEmail(config, "");
        String candidateFio = StringUtils.defaultIfBlank(req.getCandidateName(), "Кандидат");
        String title = StringUtils.defaultIfBlank(req.getTitle(), "Собеседование: " + candidateFio);

        StringBuilder desc = new StringBuilder();
        if (StringUtils.isNotBlank(req.getDescription())) {
            desc.append(escapeIcs(req.getDescription())).append("\\n\\n");
        }
        if (StringUtils.isNotBlank(telemostUrl)) {
            desc.append("Ссылка на видеовстречу Яндекс Телемост: ").append(telemostUrl).append("\\n");
            if (req.isTelemostAutoRecord()) {
                desc.append("Внимание: видеовстреча будет автоматически записана для протокола собеседования.\\n");
            }
            if (req.isTelemostAiSummary()) {
                desc.append("Включено автоматическое создание AI-конспекта встречи.\\n");
            }
        }
        desc.append("Часовой пояс встречи: ").append(timeZone).append(" (для Москвы: UTC+3, для Саратова: UTC+4)\\n");

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//HUNTTECH//Recruiting HRM 1.0//RU\r\n");
        ics.append("CALSCALE:GREGORIAN\r\n");
        ics.append("METHOD:REQUEST\r\n");
        ics.append("BEGIN:VEVENT\r\n");
        ics.append("UID:").append(uid).append("\r\n");
        ics.append("DTSTAMP:").append(nowUtc).append("\r\n");
        ics.append("DTSTART;TZID=").append(timeZone).append(":").append(startStr).append("\r\n");
        ics.append("DTEND;TZID=").append(timeZone).append(":").append(endStr).append("\r\n");
        ics.append("SUMMARY:").append(escapeIcs(title)).append("\r\n");
        ics.append("DESCRIPTION:").append(desc.toString()).append("\r\n");
        if (StringUtils.isNotBlank(telemostUrl)) {
            ics.append("LOCATION:").append(escapeIcs(telemostUrl)).append("\r\n");
        }
        ics.append("ORGANIZER:mailto:").append(organizerEmail).append("\r\n");

        if (StringUtils.isNotBlank(req.getCandidateEmail())) {
            ics.append("ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;CN=").append(escapeIcs(candidateFio))
                    .append(":mailto:").append(req.getCandidateEmail().trim()).append("\r\n");
        }
        for (String email : req.getAttendeeEmails()) {
            if (StringUtils.isNotBlank(email) && !email.equalsIgnoreCase(req.getCandidateEmail())) {
                ics.append("ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION:mailto:").append(email.trim()).append("\r\n");
            }
        }

        ics.append("STATUS:CONFIRMED\r\n");
        ics.append("SEQUENCE:0\r\n");
        ics.append("END:VEVENT\r\n");
        ics.append("END:VCALENDAR\r\n");

        return ics.toString();
    }

    private String escapeIcs(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    private String normalizeUrl(String base, String path) {
        if (path == null) path = "";
        if (base.endsWith("/") && path.startsWith("/")) {
            return base + path.substring(1);
        } else if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        } else {
            return base + path;
        }
    }

    private Map<String, String> buildWikiHeaders(UserYandexConfiguration config) {
        Map<String, String> headers = new HashMap<>();
        if (StringUtils.isNotBlank(config.getWikiCollabId())) {
            headers.put("X-Collab-Org-Id", config.getWikiCollabId().trim());
        }
        return headers;
    }

    private String extractTagContent(String xml, String tagName) {
        if (xml == null) return null;
        Pattern p = Pattern.compile("<(?:\\w+:)?" + tagName + "[^>]*>(.*?)</(?:\\w+:)?" + tagName + ">", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    protected HttpResult sendHttp(String method, String urlStr, String token, String body, String contentType, Map<String, String> extraHeaders) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        setRequestMethodSafely(conn, method);
        conn.setConnectTimeout(HTTP_TIMEOUT_MS);
        conn.setReadTimeout(HTTP_TIMEOUT_MS);
        conn.setDoInput(true);

        if (StringUtils.isNotBlank(token)) {
            conn.setRequestProperty("Authorization", "OAuth " + token);
        }
        if (StringUtils.isNotBlank(contentType)) {
            conn.setRequestProperty("Content-Type", contentType);
        }
        if (extraHeaders != null) {
            for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        if (body != null && ("POST".equals(method) || "PUT".equals(method) || "PROPFIND".equals(method) || "REPORT".equals(method))) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
        String responseBody = "";
        if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                responseBody = sb.toString();
            }
        }
        return new HttpResult(code, responseBody);
    }

    @Override
    public List<YandexCalendarInfoDto> getAvailableCalendars(UUID userId) {
        if (userId == null) {
            userId = userSessionSource.getUserSession().getUser().getId();
        }

        List<YandexCalendarInfoDto> result = new ArrayList<>();
        UserYandexConfiguration userConfig = getOrCreateConfiguration(userId);

        // 1. Личный календарь пользователя
        boolean hasPersonalDefault = false;
        String personalPath = userConfig.getPersonalCalendarPath();
        String personalName = StringUtils.defaultIfBlank(userConfig.getPersonalCalendarName(), "Личный календарь");
        String resolvedToken = resolveToken(userConfig);
        if (StringUtils.isNotBlank(personalPath) || Boolean.TRUE.equals(userConfig.getCalendarConnected()) || StringUtils.isNotBlank(resolvedToken)) {
            String path = StringUtils.isNotBlank(personalPath) ? personalPath : "/calendars/" + resolveAccountEmail(userConfig, resolvedToken) + "/events/";
            hasPersonalDefault = StringUtils.isNotBlank(personalPath);
            result.add(new YandexCalendarInfoDto("personal", personalName, path, hasPersonalDefault, false, true));
        }

        // 2. Активные корпоративные календари
        List<CorporateYandexCalendar> corpList = dataManager.load(CorporateYandexCalendar.class)
                .query("select e from hunttech_CorporateYandexCalendar e where e.active = true order by e.isDefault desc, e.name asc")
                .view("corporateYandexCalendar-view")
                .list();

        for (CorporateYandexCalendar c : corpList) {
            String name = c.getName() + " (Корпоративный)";
            result.add(new YandexCalendarInfoDto(c.getId().toString(), name, c.getCalendarPath(), Boolean.TRUE.equals(c.getIsDefault()), false, false));
        }

        if (result.isEmpty()) {
            return result;
        }

        // 3. Вычисление календаря по умолчанию по строгому приоритету:
        // 1) персональный календарь, если он явно настроен как default (personalCalendarPath не пустой)
        // 2) иначе корпоративный календарь с isDefault = true
        // 3) иначе первый доступный активный календарь
        YandexCalendarInfoDto defaultCal = null;
        if (hasPersonalDefault && !result.isEmpty() && result.get(0).isPersonal()) {
            defaultCal = result.get(0);
        } else {
            for (YandexCalendarInfoDto dto : result) {
                if (!dto.isPersonal() && dto.isDefault()) {
                    defaultCal = dto;
                    break;
                }
            }
        }
        if (defaultCal == null && !result.isEmpty()) {
            defaultCal = result.get(0);
        }

        for (YandexCalendarInfoDto dto : result) {
            dto.setDefault(dto == defaultCal);
        }

        return result;
    }

    @Override
    public YandexMeetingResult syncInteractionCalendarEvent(UUID userId, UUID iteractionListId, boolean addToCalendar, String selectedCalendarPath, String userTimeZoneId) {
        if (userId == null) {
            userId = userSessionSource.getUserSession().getUser().getId();
        }
        if (iteractionListId == null) {
            return YandexMeetingResult.error("Идентификатор взаимодействия не указан");
        }

        IteractionList iteraction = dataManager.load(IteractionList.class)
                .id(iteractionListId)
                .view(com.haulmont.cuba.core.global.ViewBuilder.of(IteractionList.class)
                        .addView("iteractionList-edit-view")
                        .add("comment")
                        .build())
                .optional()
                .orElse(null);

        if (iteraction == null) {
            return YandexMeetingResult.error("Взаимодействие не найдено в базе данных: " + iteractionListId);
        }

        // Сценарий 1: Календарная синхронизация отключена (addToCalendar == false)
        if (!addToCalendar) {
            boolean hasCalendarData = StringUtils.isNotBlank(iteraction.getCalendarEventId())
                    || StringUtils.isNotBlank(iteraction.getCalendarId())
                    || Boolean.TRUE.equals(iteraction.getAddToCalendar());
            if (hasCalendarData) {
                if (StringUtils.isNotBlank(iteraction.getCalendarEventId()) && StringUtils.isNotBlank(iteraction.getCalendarId())) {
                    try {
                        cancelCalendarEvent(userId, iteraction.getCalendarId(), iteraction.getCalendarEventId());
                    } catch (Exception e) {
                        log.warn("Не удалось удалить событие {} из CalDAV: {}", iteraction.getCalendarEventId(), e.getMessage());
                    }
                }
                iteraction.setCalendarEventId(null);
                iteraction.setCalendarId(null);
                iteraction.setCalendarSyncState(null);
                iteraction.setAddToCalendar(false);
                dataManager.commit(iteraction);
            }
            return YandexMeetingResult.success(null, null, null, "Событие отключено от календаря");
        }

        // Сценарий 2: Синхронизация включена (addToCalendar == true)
        Date startTime = iteraction.getAddDate();
        if (startTime == null) {
            return YandexMeetingResult.error("Дата и время взаимодействия не указаны");
        }

        String targetCalendarPath = selectedCalendarPath;
        if (StringUtils.isBlank(targetCalendarPath)) {
            targetCalendarPath = iteraction.getCalendarId();
        }
        if (StringUtils.isBlank(targetCalendarPath)) {
            List<YandexCalendarInfoDto> available = getAvailableCalendars(userId);
            for (YandexCalendarInfoDto c : available) {
                if (c.isDefault()) {
                    targetCalendarPath = c.getPath();
                    break;
                }
            }
            if (StringUtils.isBlank(targetCalendarPath) && !available.isEmpty()) {
                targetCalendarPath = available.get(0).getPath();
            }
        }

        if (StringUtils.isBlank(targetCalendarPath)) {
            return YandexMeetingResult.error("Нет доступных календарей для добавления события");
        }

        // Если сменился календарь — удаляем событие из старого календаря и сбрасываем UID для создания в новом
        String previousCalendarPath = iteraction.getCalendarId();
        String previousEventUid = iteraction.getCalendarEventId();
        if (StringUtils.isNotBlank(previousEventUid)
                && (StringUtils.isBlank(previousCalendarPath) || !previousCalendarPath.equalsIgnoreCase(targetCalendarPath))) {
            if (StringUtils.isNotBlank(previousCalendarPath)) {
                cancelCalendarEvent(userId, previousCalendarPath, previousEventUid);
            }
            previousEventUid = null;
        }

        // Формирование названия события по стандарту ТЗ:
        // <Имя кандидата> — <Должность кандидата> — <Название взаимодействия>
        String candidateName = null;
        String candidatePosition = null;
        String candidateEmail = null;
        UUID candidateId = null;

        if (iteraction.getCandidate() != null) {
            candidateId = iteraction.getCandidate().getId();
            candidateName = StringUtils.trimToNull(iteraction.getCandidate().getFullName());
            candidateEmail = StringUtils.trimToNull(iteraction.getCandidate().getEmail());
            if (iteraction.getCandidate().getPersonPosition() != null) {
                candidatePosition = StringUtils.trimToNull(iteraction.getCandidate().getPersonPosition().getPositionRuName());
                if (candidatePosition == null) {
                    candidatePosition = StringUtils.trimToNull(iteraction.getCandidate().getPersonPosition().getPositionEnName());
                }
            }
        }
        if (candidatePosition == null && iteraction.getVacancy() != null) {
            candidatePosition = StringUtils.trimToNull(iteraction.getVacancy().getVacansyName());
        }

        String interactionName = null;
        if (iteraction.getIteractionType() != null) {
            interactionName = StringUtils.trimToNull(iteraction.getIteractionType().getIterationName());
        }

        List<String> titleParts = new ArrayList<>();
        if (candidateName != null) titleParts.add(candidateName);
        if (candidatePosition != null) titleParts.add(candidatePosition);
        if (interactionName != null) titleParts.add(interactionName);

        String eventTitle = titleParts.isEmpty() ? "Взаимодействие с кандидатом" : String.join(" — ", titleParts);

        // Расчет времени (1 час длительности по умолчанию)
        Date endTime = new Date(startTime.getTime() + 60 * 60 * 1000L);

        // TimeZone
        UserYandexConfiguration userConfig = getOrCreateConfiguration(userId);
        String timeZone = StringUtils.defaultIfBlank(userTimeZoneId,
                StringUtils.defaultIfBlank(userConfig.getDefaultTimeZone(), DEFAULT_TIME_ZONE_SARATOV));

        YandexMeetingRequest request = new YandexMeetingRequest();
        request.setEventUid(previousEventUid);
        request.setCustomCalendarPath(targetCalendarPath);
        request.setTitle(eventTitle);
        request.setDescription(iteraction.getComment());
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setTimeZone(timeZone);
        request.setCandidateId(candidateId);
        request.setCandidateName(candidateName);
        request.setCandidateEmail(candidateEmail);
        request.setCreateTelemostMeeting(true);
        if (iteraction.getVacancy() != null) {
            request.setOpenPositionId(iteraction.getVacancy().getId());
        }

        try {
            YandexMeetingResult result = scheduleCalendarEvent(userId, request);
            if (result.isSuccess()) {
                iteraction.setCalendarEventId(result.getEventUid());
                iteraction.setCalendarId(targetCalendarPath);
                iteraction.setCalendarSyncState("SYNCED");
                iteraction.setAddToCalendar(true);
                dataManager.commit(iteraction);
                return result;
            } else {
                iteraction.setCalendarSyncState("FAILED");
                iteraction.setAddToCalendar(true);
                dataManager.commit(iteraction);
                return result;
            }
        } catch (Exception ex) {
            log.error("Сбой синхронизации с Яндекс Календарём для взаимодействия {}: {}", iteractionListId, ex.getMessage(), ex);
            try {
                iteraction.setCalendarSyncState("FAILED");
                iteraction.setAddToCalendar(true);
                dataManager.commit(iteraction);
            } catch (Exception ignore) {}
            return YandexMeetingResult.error("Взаимодействие сохранено, но событие не удалось добавить в Яндекс Календарь: " + ex.getMessage());
        }
    }

    private void setRequestMethodSafely(HttpURLConnection conn, String method) throws Exception {
        try {
            conn.setRequestMethod(method);
        } catch (java.net.ProtocolException pe) {
            // Разрешаем CalDAV/WebDAV методы (PROPFIND, REPORT, MKCALENDAR), которые HttpURLConnection блокирует
            boolean set = false;
            Class<?> c = conn.getClass();
            while (c != null && c != Object.class) {
                try {
                    java.lang.reflect.Field field = c.getDeclaredField("method");
                    field.setAccessible(true);
                    field.set(conn, method);
                    set = true;
                    break;
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
            if (!set) {
                try {
                    java.lang.reflect.Field field = HttpURLConnection.class.getDeclaredField("method");
                    field.setAccessible(true);
                    field.set(conn, method);
                } catch (Exception ignored) {
                    throw pe;
                }
            }
        }
    }

    protected static class HttpResult {
        protected final int statusCode;
        protected final String body;

        public HttpResult(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    private static class CalendarConnectionException extends RuntimeException {
        private final int statusCode;

        CalendarConnectionException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        int getStatusCode() {
            return statusCode;
        }
    }
}
