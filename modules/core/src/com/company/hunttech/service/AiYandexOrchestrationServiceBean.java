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
            "(встреч|собеседован|календар|созвон|телемост|интервью|звонок|запланируй|поставь встречу|создай событие)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern CLIENT_CALENDAR_PATTERN = Pattern.compile(
            "(заказчик|клиент|у заказчика|с заказчиком|для заказчика)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern PERSONAL_CALENDAR_PATTERN = Pattern.compile(
            "(в моем календаре|мой календарь|в личном календаре|личный календарь|мне в календарь|для меня)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?:в\\s+)?(\\d{1,2})[:.](\\d{2})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern HOUR_ONLY_PATTERN = Pattern.compile(
            "(?:в\\s+)?(\\d{1,2})\\s*(?:час|ч|:00)",
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

        // 1. Различение типа календаря: корпоративный «Hunttech у заказчика» vs личный
        if (CLIENT_CALENDAR_PATTERN.matcher(userMessage).find()) {
            result.setCalendarType(YandexCalendarType.CLIENT_INTERVIEW);
            result.setCalendarName(UserYandexConfiguration.DEFAULT_CLIENT_CALENDAR_NAME);
            result.setExplanation("Определен корпоративный календарь собеседований с заказчиком ('" +
                    UserYandexConfiguration.DEFAULT_CLIENT_CALENDAR_NAME + "')");
        } else if (PERSONAL_CALENDAR_PATTERN.matcher(userMessage).find()) {
            result.setCalendarType(YandexCalendarType.PERSONAL);
            result.setCalendarName("Личный календарь");
            result.setExplanation("Определен личный календарь пользователя по запросу");
        } else {
            result.setCalendarType(YandexCalendarType.PERSONAL);
            result.setCalendarName("Личный календарь");
            result.setExplanation("Выбран личный календарь пользователя по умолчанию");
        }

        // 2. Определение кандидата
        findCandidateInMessage(userMessage, result);

        // 3. Определение даты и времени встречи
        resolveMeetingDateTime(userMessage, result);

        // 4. Определение Телемоста
        boolean isOffline = OFFLINE_PATTERN.matcher(userMessage).find();
        boolean mentionsTelemost = TELEMOST_PATTERN.matcher(userMessage).find();
        result.setTelemostRequired(!isOffline && (mentionsTelemost || result.getCalendarType() == YandexCalendarType.CLIENT_INTERVIEW));
        result.setTelemostAutoRecord(true);
        result.setTelemostAiSummary(true);

        // 5. Формирование заголовка и описания
        String candidatePart = result.getCandidateFio() != null ? result.getCandidateFio() : "Кандидат";
        String titlePrefix = result.getCalendarType() == YandexCalendarType.CLIENT_INTERVIEW
                ? "Собеседование у заказчика: "
                : "Собеседование: ";
        result.setTitle(titlePrefix + candidatePart);

        StringBuilder desc = new StringBuilder();
        desc.append("Собеседование с кандидатом ").append(candidatePart).append(".\n");
        if (StringUtils.isNotBlank(result.getCandidateEmail())) {
            desc.append("Email кандидата: ").append(result.getCandidateEmail()).append("\n");
        }
        desc.append("Организатор: HRM HuntTech.\n");
        result.setDescription(desc.toString());

        return result;
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

        // 3. Если встреча создана успешно — сохраняем запись в IteractionList
        if (result.isSuccess() && parsedIntent.getCandidateId() != null) {
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
        userReport.append("✅ **Событие успешно запланировано!**\n\n");
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
        userReport.append("- **Приглашения:** Отправлены участникам через CalDAV outbox.\n");

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

    private void resolveMeetingDateTime(String msg, AiMeetingParseResult result) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Saratov"));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        String lower = msg.toLowerCase();
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

        int hour = 15;
        int minute = 0;

        Matcher tm = TIME_PATTERN.matcher(msg);
        if (tm.find()) {
            hour = Integer.parseInt(tm.group(1));
            minute = Integer.parseInt(tm.group(2));
        } else {
            Matcher hm = HOUR_ONLY_PATTERN.matcher(msg);
            if (hm.find()) {
                hour = Integer.parseInt(hm.group(1));
            }
        }

        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);

        Date start = cal.getTime();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        Date end = cal.getTime();

        result.setStartTime(start);
        result.setEndTime(end);
        result.setTimeZone("Europe/Saratov");
    }
}
