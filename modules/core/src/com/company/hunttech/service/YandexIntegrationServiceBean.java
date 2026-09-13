package com.company.hunttech.service;

import com.company.hunttech.core.ai.AiSecretService;
import com.company.hunttech.dto.yandex.*;
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
    public YandexDiagnosticResult testConnection(UUID userId, String serviceType) {
        UserYandexConfiguration config = getOrCreateConfiguration(userId);
        String token = resolveToken(config);
        if (StringUtils.isBlank(token)) {
            return YandexDiagnosticResult.error(serviceType, 401, "Не указан OAuth токен авторизации Яндекс 360", null);
        }

        try {
            if ("CALENDAR".equalsIgnoreCase(serviceType)) {
                List<YandexCalendarInfoDto> calendars = discoverCalendarsInternal(config, token);
                boolean foundClient = calendars.stream().anyMatch(YandexCalendarInfoDto::isClientInterviewCalendar);
                String msg = String.format("Календари доступны. Обнаружено %d календарей (в т.ч. 'Hunttech у заказчика': %s)",
                        calendars.size(), foundClient ? "найден" : "не найден, будет создан или использован личный");
                config.setCalendarConnected(true);
                config.setLastVerifiedAt(new Date());
                config.setLastVerificationMessage(msg);
                dataManager.commit(config);
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
        } catch (Exception ex) {
            log.error("Ошибка при проверке подключения к {}: {}", serviceType, ex.getMessage(), ex);
            return YandexDiagnosticResult.error(serviceType, 500, "Ошибка связи: " + ex.getMessage(), null);
        }
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
            String targetCalendarPath = resolveTargetCalendarPath(config, token, request.getCalendarType(), request.getCustomCalendarPath());
            String calendarDisplayName = request.getCalendarType() == YandexCalendarType.CLIENT_INTERVIEW
                    ? config.getClientInterviewCalendarName()
                    : StringUtils.defaultIfBlank(config.getPersonalCalendarName(), "Основной календарь");

            // 3. Формируем UID и iCalendar
            String eventUid = UUID.randomUUID().toString();
            String timeZone = StringUtils.defaultIfBlank(request.getTimeZone(),
                    StringUtils.defaultIfBlank(config.getDefaultTimeZone(), DEFAULT_TIME_ZONE_SARATOV));

            String icsBody = buildIcsContent(eventUid, request, config, telemostJoinUrl, timeZone);

            // 4. Запись события в CalDAV через PUT
            String baseUrl = StringUtils.defaultIfBlank(config.getCalendarBaseUrl(), UserYandexConfiguration.DEFAULT_CALENDAR_BASE_URL);
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
                String account = resolveAccountEmail(config, token);
                String outboxUrl = normalizeUrl(baseUrl, "/calendars/" + URLEncoder.encode(account, StandardCharsets.UTF_8.name()) + "/outbox/");
                Map<String, String> outboxHeaders = new HashMap<>();
                outboxHeaders.put("Origin", "https://calendar.yandex.ru");
                try {
                    HttpResult outboxResult = sendHttp("POST", outboxUrl, token, icsBody, "text/calendar; charset=utf-8", outboxHeaders);
                    log.info("CalDAV outbox результат: HTTP {}", outboxResult.statusCode);
                } catch (Exception ex) {
                    log.warn("Ошибка отправки инвайтов в outbox: {}", ex.getMessage());
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
        UserYandexConfiguration config = getOrCreateConfiguration(userId);
        String token = resolveToken(config);
        if (StringUtils.isBlank(token) || StringUtils.isBlank(calendarPath) || StringUtils.isBlank(eventUid)) {
            return false;
        }
        try {
            String baseUrl = StringUtils.defaultIfBlank(config.getCalendarBaseUrl(), UserYandexConfiguration.DEFAULT_CALENDAR_BASE_URL);
            String eventUrl = normalizeUrl(baseUrl, calendarPath) + "/" + eventUid + ".ics";
            HttpResult res = sendHttp("DELETE", eventUrl, token, null, null, null);
            return res.statusCode == 200 || res.statusCode == 204;
        } catch (Exception ex) {
            log.error("Ошибка удаления события {}: {}", eventUid, ex.getMessage(), ex);
            return false;
        }
    }

    // --- Внутренние вспомогательные методы ---

    private String resolveToken(UserYandexConfiguration config) {
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

    private String resolveAccountEmail(UserYandexConfiguration config, String token) {
        if (StringUtils.isNotBlank(config.getAccountEmail())) {
            return config.getAccountEmail().trim();
        }
        try {
            HttpResult res = sendHttp("GET", "https://login.yandex.ru/info?format=json", token, null, "application/json", null);
            if (res.statusCode == 200) {
                JSONObject json = new JSONObject(res.body);
                String email = json.optString("default_email", json.optString("login", "user"));
                config.setAccountEmail(email);
                dataManager.commit(config);
                return email;
            }
        } catch (Exception ignored) {}
        return "user@yandex.ru";
    }

    private List<YandexCalendarInfoDto> discoverCalendarsInternal(UserYandexConfiguration config, String token) {
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

            // Разбираем ответы
            Pattern responsePattern = Pattern.compile("<(?:\\w+:)?response>(.*?)</(?:\\w+:)?response>", Pattern.DOTALL);
            Matcher matcher = responsePattern.matcher(listRes.body);

            while (matcher.find()) {
                String responseXml = matcher.group(1);
                if (responseXml.contains("<resourcetype") && (responseXml.contains("calendar") || responseXml.contains("collection"))) {
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
                // Фолбэк на стандартный путь календаря пользователя
                String defaultPath = "/calendars/" + URLEncoder.encode(account, StandardCharsets.UTF_8.name()) + "/events/";
                result.add(new YandexCalendarInfoDto("default", "Основной", defaultPath, true, false));
                String clientPath = "/calendars/" + URLEncoder.encode(account, StandardCharsets.UTF_8.name()) + "/hunttech-client/";
                result.add(new YandexCalendarInfoDto("client", UserYandexConfiguration.DEFAULT_CLIENT_CALENDAR_NAME, clientPath, false, true));
            }

        } catch (Exception ex) {
            log.warn("Ошибка CalDAV discovery: {}", ex.getMessage());
            // Фолбэк
            String defaultPath = "/calendars/" + account + "/events/";
            result.add(new YandexCalendarInfoDto("default", "Основной календарь", defaultPath, true, false));
        }

        return result;
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

    private String buildIcsContent(String uid, YandexMeetingRequest req, UserYandexConfiguration config, String telemostUrl, String timeZone) {
        SimpleDateFormat utcFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat localFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        localFormat.setTimeZone(TimeZone.getTimeZone(timeZone));

        String nowUtc = utcFormat.format(new Date());
        String startStr = localFormat.format(req.getStartTime() != null ? req.getStartTime() : new Date());
        String endStr = localFormat.format(req.getEndTime() != null ? req.getEndTime() : new Date(System.currentTimeMillis() + 3600000));

        String organizerEmail = resolveAccountEmail(config, "");
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

    private HttpResult sendHttp(String method, String urlStr, String token, String body, String contentType, Map<String, String> extraHeaders) throws Exception {
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

        if (body != null && ("POST".equals(method) || "PUT".equals(method) || "PROPFIND".equals(method))) {
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

    private static class HttpResult {
        final int statusCode;
        final String body;

        HttpResult(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }
}
