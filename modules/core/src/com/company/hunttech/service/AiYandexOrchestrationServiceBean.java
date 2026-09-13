package com.company.hunttech.service;

import com.company.hunttech.dto.yandex.*;
import com.company.hunttech.entity.*;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(AiYandexOrchestrationService.NAME)
public class AiYandexOrchestrationServiceBean implements AiYandexOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(AiYandexOrchestrationServiceBean.class);

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private YandexIntegrationService yandexIntegrationService;
    @Inject
    private UserSessionSource userSessionSource;

    private static final Pattern MEETING_INTENT_PATTERN = Pattern.compile(
            "(встреч|собеседован|календар|созвон|звонок|телемост|интервью|запланируй|поставь\\s+встречу|" +
            AiYandexOrchestrationService.BOOKING_VERBS_REGEX + "\\s+(?:[\\wа-яё]+\\s+){0,4}(?:в\\s+календар[ея]|событи[еяй]|встреч[ауие]?|созвон|звонок|митинг|инвайт)|" +
            "событи[еяй].*(?:календар|яндекс)|инвайт)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern CLIENT_CALENDAR_PATTERN = Pattern.compile(
            "(заказчик|клиент|у заказчика|с заказчиком|для заказчика)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern PERSONAL_CALENDAR_PATTERN = Pattern.compile(
            "(?:в\\s+мо[её]м\\s+календаре|мой\\s+календарь|в\\s+личном\\s+календаре|личный\\s+календарь)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?:(?:в|на)\\s+)?(?<![\\d:])([01]?\\d|2[0-3]):([0-5]\\d)(?![\\d:])|" +
            "(?:(?:в|на)\\s+)?(?<![\\d.\\-])([01]?\\d|2[0-3])[.\\-]([0-5]\\d)(?![:.\\-]?\\d)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern HOUR_ONLY_PATTERN = Pattern.compile(
            "(?:в)?\\s*([01]?\\d|2[0-3])\\s*(?:час(?:а|ов)?|ч|:00)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(?:длительностью|продолжительностью)\\s+(\\d{1,4})\\s*(час(?:а|ов)?|ч(?![\\p{L}])|минут(?:ы)?|мин(?![\\p{L}]))|(?:на)\\s+(\\d{1,3})\\s*(минут(?:ы)?|мин(?![\\p{L}]))",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern QUOTED_TOPIC_PATTERN = Pattern.compile(
            "[\"«]([^\"»\n]+)[\"»]",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern TOPIC_KEYWORD_PATTERN = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:тема|тему|с\\s+темой|на\\s+тему|название|с\\s+названием)[:\\s]+(?:[\"«]([^\"»\n]+)[\"»]|([^,\n]+?(?=\\s+(?:на\\s+\\d|в\\s+\\d|сегодня|завтра|послезавтра|с\\s+кандидатом|длительностью|продолжительностью)|[,;\n]|$)))",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern INLINE_DATE_TIME_PATTERN = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:сегодня|завтра|послезавтра|в\\s+\\d{1,2}[:.\\-]\\d{2}|на\\s+\\d{1,2}[:.\\-]\\d{2}|(?<![\\p{L}\\p{N}])\\d{1,2}:\\d{2}(?![\\p{L}\\p{N}])|в\\s+\\d{1,2}\\s*(?:час|ч)|на\\s+\\d{1,2}\\s*(?:час|ч)|понедельник|вторник|среду|четверг|пятницу|субботу|воскресенье)(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern MOSCOW_TZ_PATTERN = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:мск|по\\s+москв[а-яё]*|по\\s+московскому\\s+времени)(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern SAMARA_TZ_PATTERN = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:по\\s+самар[а-яё]*|по\\s+самарскому\\s+времени)(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern SARATOV_TZ_PATTERN = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:по\\s+саратов[а-яё]*|по\\s+саратовскому\\s+времени)(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern CANDIDATE_NAME_PATTERN = Pattern.compile(
            "(?:с\\s+кандидатом|кандидат(?:а|ом)?|с)\\s+([А-ЯЁ][а-яё]+(?:\\s+[А-ЯЁ][а-яё]+){1,2})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern OFFLINE_PATTERN = Pattern.compile(
            "(очно|в офисе|личное присутствие)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern TELEMOST_PATTERN = Pattern.compile(
            "(телемост|видео|онлайн|видеозвонок|созвон|дистанцион|встреч|собеседован|интервью|звонок)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    @Override
    public boolean isMeetingBookingIntent(String userMessage) {
        if (StringUtils.isBlank(userMessage)) {
            return false;
        }
        return MEETING_INTENT_PATTERN.matcher(userMessage).find();
    }

    @Override
    public AiMeetingParseResult parseMeetingIntent(String userMessage, UUID currentUserId) {
        AiMeetingParseResult result = new AiMeetingParseResult();
        result.setRawUserMessage(userMessage);

        if (StringUtils.isBlank(userMessage)) {
            result.setIntentDetected(false);
            return result;
        }

        boolean hasIntent = MEETING_INTENT_PATTERN.matcher(userMessage).find();
        result.setIntentDetected(hasIntent);
        if (!hasIntent) {
            return result;
        }

        // Загрузка персональной конфигурации Yandex для текущего пользователя (read-only, без создания записи в БД)
        UserYandexConfiguration userYandexConfig = null;
        if (currentUserId != null) {
            try {
                userYandexConfig = dataManager.load(UserYandexConfiguration.class)
                        .query("select c from hunttech_UserYandexConfiguration c where c.user.id = :userId and c.deleteTs is null")
                        .parameter("userId", currentUserId)
                        .view("userYandexConfiguration-view")
                        .optional()
                        .orElse(null);
            } catch (Exception e) {
                log.warn("Не удалось получить UserYandexConfiguration для userId={}: {}", currentUserId, e.getMessage());
            }
        }

        // 1. Различение типа календаря: корпоративный «Hunttech у заказчика» vs личный
        boolean isClientCalendar = CLIENT_CALENDAR_PATTERN.matcher(userMessage).find();
        boolean isExplicitPersonal = PERSONAL_CALENDAR_PATTERN.matcher(userMessage).find();
        if (isClientCalendar && !isExplicitPersonal) {
            result.setCalendarType(YandexCalendarType.CLIENT_INTERVIEW);
            String clientCalName = (userYandexConfig != null && StringUtils.isNotBlank(userYandexConfig.getClientInterviewCalendarName()))
                    ? userYandexConfig.getClientInterviewCalendarName()
                    : UserYandexConfiguration.DEFAULT_CLIENT_CALENDAR_NAME;
            result.setCalendarName(clientCalName);
            result.setExplanation("Определен корпоративный календарь собеседований с заказчиком ('" + clientCalName + "')");
        } else {
            result.setCalendarType(YandexCalendarType.PERSONAL);
            String personalCalName = (userYandexConfig != null && StringUtils.isNotBlank(userYandexConfig.getPersonalCalendarName()))
                    ? userYandexConfig.getPersonalCalendarName()
                    : YandexIntegrationService.DEFAULT_PERSONAL_CALENDAR_NAME;
            result.setCalendarName(personalCalName);
            result.setExplanation("Определен личный календарь пользователя ('" + personalCalName + "')");
        }

        // 2. Определение кандидата
        findCandidateInMessage(userMessage, result);

        // 3. Определение даты, времени и часового пояса
        resolveMeetingDateTime(userMessage, result, userYandexConfig);

        // 4. Определение темы (Summary/Title) и описания
        String customTopic = extractCustomTopic(userMessage);
        boolean isCandidateFound = result.getCandidateFio() != null;
        if (isCandidateFound) {
            String candidatePart = result.getCandidateFio();
            String titlePrefix = result.getCalendarType() == YandexCalendarType.CLIENT_INTERVIEW
                    ? "Собеседование у заказчика: "
                    : "Собеседование: ";
            boolean customTopicIsCandidateName = StringUtils.isNotBlank(customTopic)
                    && (customTopic.equalsIgnoreCase(candidatePart) || candidatePart.contains(customTopic) || customTopic.contains(candidatePart));
            if (StringUtils.isNotBlank(customTopic) && !customTopicIsCandidateName) {
                result.setTitle(titlePrefix + candidatePart + " (" + customTopic + ")");
            } else {
                result.setTitle(titlePrefix + candidatePart);
            }
            StringBuilder desc = new StringBuilder();
            desc.append("Собеседование с кандидатом ").append(candidatePart).append(".\n");
            if (StringUtils.isNotBlank(customTopic) && !customTopicIsCandidateName) {
                desc.append("Тема: ").append(customTopic).append(".\n");
            }
            if (StringUtils.isNotBlank(result.getCandidateEmail())) {
                desc.append("Email кандидата: ").append(result.getCandidateEmail()).append("\n");
            }
            desc.append("Организатор: HRM HuntTech.\n");
            result.setDescription(desc.toString());
        } else if (StringUtils.isNotBlank(customTopic)) {
            result.setTitle(customTopic.trim());
            result.setDescription(customTopic.trim() + "\n\nСоздано через HRM HuntTech (Яндекс 360).");
        } else {
            String defaultTitle = result.getCalendarType() == YandexCalendarType.CLIENT_INTERVIEW
                    ? "Собеседование у заказчика"
                    : "Событие в календаре";
            result.setTitle(defaultTitle);
            result.setDescription(defaultTitle);
        }

        // 5. Определение Телемоста
        boolean isOffline = OFFLINE_PATTERN.matcher(userMessage).find();
        boolean mentionsTelemost = TELEMOST_PATTERN.matcher(userMessage).find();
        boolean isClientInterview = result.getCalendarType() == YandexCalendarType.CLIENT_INTERVIEW;
        boolean needTelemost = !isOffline && (mentionsTelemost || isClientInterview);
        result.setTelemostRequired(needTelemost);
        result.setTelemostAutoRecord(true);
        result.setTelemostAiSummary(true);

        return result;
    }

    private String extractCustomTopic(String msg) {
        Matcher qm = QUOTED_TOPIC_PATTERN.matcher(msg);
        if (qm.find()) {
            String val = qm.group(1).trim();
            val = INLINE_DATE_TIME_PATTERN.matcher(val).replaceAll(" ").trim().replaceAll("\\s+", " ");
            if (!val.isEmpty()) {
                return val;
            }
        }
        Matcher km = TOPIC_KEYWORD_PATTERN.matcher(msg);
        if (km.find()) {
            String rawVal = km.group(1) != null ? km.group(1) : km.group(2);
            if (rawVal != null) {
                String val = rawVal.trim();
                val = INLINE_DATE_TIME_PATTERN.matcher(val).replaceAll(" ").trim().replaceAll("\\s+", " ");
                if (!val.isEmpty()) {
                    return val;
                }
            }
        }
        return null;
    }

    @Override
    public YandexMeetingResult executeMeetingBooking(UUID currentUserId, AiMeetingParseResult parsedIntent) {
        if (currentUserId == null) {
            currentUserId = userSessionSource.getUserSession().getUser().getId();
        }
        ExtUser currentUser = dataManager.load(ExtUser.class).id(currentUserId).view("extUser-view").one();

        // 1. Формируем запрос к YandexIntegrationService
        YandexMeetingRequest req = new YandexMeetingRequest();
        req.setCalendarType(parsedIntent.getCalendarType());
        req.setTitle(parsedIntent.getTitle());
        req.setDescription(parsedIntent.getDescription());
        req.setStartTime(parsedIntent.getStartTime());
        req.setEndTime(parsedIntent.getEndTime());
        req.setTimeZone(parsedIntent.getTimeZone());

        req.setCandidateId(parsedIntent.getCandidateId());
        req.setCandidateName(parsedIntent.getCandidateFio());
        req.setCandidateEmail(parsedIntent.getCandidateEmail());

        req.setCreateTelemostMeeting(parsedIntent.isTelemostRequired());
        req.setTelemostAutoRecord(parsedIntent.isTelemostAutoRecord());
        req.setTelemostAiSummary(parsedIntent.isTelemostAiSummary());

        if (StringUtils.isNotBlank(currentUser.getEmail())) {
            req.getAttendeeEmails().add(currentUser.getEmail());
        }

        // 2. Создаем событие в календаре через YandexIntegrationService
        YandexMeetingResult result = yandexIntegrationService.scheduleCalendarEvent(currentUserId, req);
        if (!result.isSuccess()) {
            log.warn("Не удалось создать событие в календаре: {}", result.getMessage());
            return result;
        }

        // 3. Если встреча создана успешно — сохраняем запись в IteractionList
        if (parsedIntent.getCandidateId() != null) {
            try {
                JobCandidate candidate = dataManager.load(JobCandidate.class)
                        .id(parsedIntent.getCandidateId())
                        .view("_minimal")
                        .optional()
                        .orElse(null);

                if (candidate != null) {
                    IteractionList interaction = metadata.create(IteractionList.class);
                    interaction.setCandidate(candidate);
                    interaction.setRecrutier(currentUser);
                    interaction.setDateIteraction(parsedIntent.getStartTime());
                    interaction.setCommunicationMethod("Яндекс Телемост");

                    StringBuilder comment = new StringBuilder();
                    comment.append("Запланировано собеседование в Яндекс.Календаре (")
                            .append(result.getCalendarName()).append(").\n");
                    if (StringUtils.isNotBlank(result.getTelemostJoinUrl())) {
                        comment.append("Ссылка на видеовстречу Телемост: ").append(result.getTelemostJoinUrl()).append("\n");
                    }
                    comment.append("UID события: ").append(result.getEventUid()).append("\n");
                    interaction.setComment(comment.toString());

                    // Пытаемся назначить тип взаимодействия «Интервью» / «Собеседование»
                    Iteraction itType = dataManager.load(Iteraction.class)
                            .query("select e from hunttech_Iteraction e where lower(e.name) like '%собеседован%' or lower(e.name) like '%интервью%'")
                            .optional()
                            .orElse(null);
                    if (itType != null) {
                        interaction.setIteractionType(itType);
                    }

                    interaction = dataManager.commit(interaction);
                    result.setIteractionListId(interaction.getId());
                    log.info("Создана запись взаимодействия IteractionList id={}", interaction.getId());
                }
            } catch (Exception ex) {
                log.warn("Не удалось создать IteractionList: {}", ex.getMessage());
            }
        }

        // 4. Формируем подробный отчет
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone(parsedIntent.getTimeZone()));

        String formattedDate = sdf.format(parsedIntent.getStartTime());
        StringBuilder userReport = new StringBuilder();
        userReport.append("✅ **Событие успешно создано в Яндекс-Календаре!**\n\n");
        userReport.append("- **Календарь:** ").append(result.getCalendarName()).append("\n");
        userReport.append("- **Тема:** ").append(parsedIntent.getTitle()).append("\n");
        userReport.append("- **Дата и время:** ").append(formattedDate).append(" (часовой пояс ").append(parsedIntent.getTimeZone()).append(")\n");
        if (parsedIntent.getCandidateFio() != null) {
            userReport.append("- **Кандидат:** ").append(parsedIntent.getCandidateFio());
            if (parsedIntent.getCandidateEmail() != null) {
                userReport.append(" (").append(parsedIntent.getCandidateEmail()).append(")");
            }
            userReport.append("\n");
        }
        if (StringUtils.isNotBlank(result.getTelemostJoinUrl())) {
            userReport.append("- **Яндекс Телемост:** [Присоединиться к встрече](").append(result.getTelemostJoinUrl()).append(")\n");
            userReport.append("- **Опции:** Включена автоматическая запись встречи и подготовка AI-конспекта.\n");
        }
        if (result.getInvitedEmails() != null && !result.getInvitedEmails().isEmpty()) {
            userReport.append("- **Приглашения:** Отправлены участникам (").append(String.join(", ", result.getInvitedEmails())).append(").\n");
        }

        result.setMessage(userReport.toString());
        return result;
    }

    private void findCandidateInMessage(String msg, AiMeetingParseResult result) {
        Matcher m = CANDIDATE_NAME_PATTERN.matcher(msg);
        String extractedName = null;
        if (m.find()) {
            extractedName = m.group(1).trim();
        }

        if (extractedName != null) {
            String[] parts = extractedName.split("\\s+");
            String p1 = parts[0];
            String p2 = parts.length > 1 ? parts[1] : null;

            if (dataManager != null) {
                List<JobCandidate> candidates;
                if (p2 != null) {
                    candidates = dataManager.load(JobCandidate.class)
                            .query("select e from hunttech_JobCandidate e where " +
                                    "(lower(e.fio) like lower(:p1) and lower(e.fio) like lower(:p2)) or " +
                                    "(lower(e.secondName) like lower(:p1) or lower(e.firstName) like lower(:p1))")
                            .parameter("p1", "%" + p1 + "%")
                            .parameter("p2", "%" + p2 + "%")
                            .view("candidate-browse-view")
                            .list();
                } else {
                    candidates = dataManager.load(JobCandidate.class)
                            .query("select e from hunttech_JobCandidate e where " +
                                    "lower(e.fio) like lower(:p1) or lower(e.secondName) like lower(:p1) or lower(e.firstName) like lower(:p1)")
                            .parameter("p1", "%" + p1 + "%")
                            .view("candidate-browse-view")
                            .list();
                }

                if (!candidates.isEmpty()) {
                    JobCandidate c = candidates.get(0);
                    result.setCandidateId(c.getId());
                    result.setCandidateFio(c.getFullName() != null ? c.getFullName() : extractedName);
                    result.setCandidateEmail(c.getEmail());
                    return;
                }
            }
            result.setCandidateFio(extractedName);
        }
    }

    private void resolveMeetingDateTime(String msg, AiMeetingParseResult result, UserYandexConfiguration userYandexConfig) {
        String tz = UserYandexConfiguration.DEFAULT_TIME_ZONE;
        if (userYandexConfig != null && StringUtils.isNotBlank(userYandexConfig.getDefaultTimeZone())) {
            String candidateTz = userYandexConfig.getDefaultTimeZone().trim();
            try {
                java.time.ZoneId zoneId = java.time.ZoneId.of(candidateTz);
                tz = zoneId.getId();
            } catch (Exception e) {
                log.warn("Некорректный часовой пояс в UserYandexConfiguration: {}, fallback на {}", candidateTz, UserYandexConfiguration.DEFAULT_TIME_ZONE);
            }
        }
        if (MOSCOW_TZ_PATTERN.matcher(msg).find()) {
            tz = "Europe/Moscow";
        } else if (SAMARA_TZ_PATTERN.matcher(msg).find()) {
            tz = "Europe/Samara";
        } else if (SARATOV_TZ_PATTERN.matcher(msg).find()) {
            tz = "Europe/Saratov";
        }

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(tz));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        String lower = msg.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("послезавтра")) {
            cal.add(Calendar.DAY_OF_YEAR, 2);
        } else if (lower.contains("завтра")) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        } else if (lower.contains("сегодня")) {
            // сегодня
        } else {
            // по умолчанию назначаем на следующий рабочий день
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // 1. Сначала вычисляем длительность встречи/события и исключаем ее фрагмент из поиска времени
        int durationMinutes = 60;
        String timeParseText = msg;
        Matcher dm = DURATION_PATTERN.matcher(msg);
        if (dm.find()) {
            try {
                String amountStr = dm.group(1) != null ? dm.group(1) : dm.group(3);
                String unit = (dm.group(2) != null ? dm.group(2) : dm.group(4)).toLowerCase(java.util.Locale.ROOT);
                int amount = Integer.parseInt(amountStr);
                if (amount > 0) {
                    amount = Math.min(amount, 1440);
                    if (unit.startsWith("час") || unit.equals("ч")) {
                        durationMinutes = Math.min(amount * 60, 1440);
                    } else if (unit.startsWith("мин")) {
                        durationMinutes = amount;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
            timeParseText = msg.substring(0, dm.start()) + " " + msg.substring(dm.end());
        }

        // 2. Определение времени старта по очищенному тексту
        int hour = 15;
        int minute = 0;

        Matcher tm = TIME_PATTERN.matcher(timeParseText);
        if (tm.find()) {
            if (tm.group(1) != null) {
                hour = Integer.parseInt(tm.group(1));
                minute = Integer.parseInt(tm.group(2));
            } else if (tm.group(3) != null) {
                hour = Integer.parseInt(tm.group(3));
                minute = Integer.parseInt(tm.group(4));
            }
        } else {
            Matcher hm = HOUR_ONLY_PATTERN.matcher(timeParseText);
            if (hm.find()) {
                hour = Integer.parseInt(hm.group(1));
            }
        }

        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);

        Date start = cal.getTime();

        cal.add(Calendar.MINUTE, durationMinutes);
        Date end = cal.getTime();

        result.setStartTime(start);
        result.setEndTime(end);
        result.setTimeZone(tz);
    }
}
