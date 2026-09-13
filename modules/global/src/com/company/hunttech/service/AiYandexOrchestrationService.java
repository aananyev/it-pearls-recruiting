package com.company.hunttech.service;

import com.company.hunttech.dto.yandex.AiMeetingParseResult;
import com.company.hunttech.dto.yandex.YandexMeetingResult;

import java.util.UUID;

public interface AiYandexOrchestrationService {
    String NAME = "hunttech_AiYandexOrchestrationService";

    String CALDAV_NOT_CONFIGURED_OR_FAILED_MESSAGE =
            "Не удалось создать событие в Яндекс-Календаре. Проверьте настройки подключения к Яндекс 360 в окне Настроек (вкладка «Яндекс-360») и корректность учетных данных.";

    static String formatCalDavFailureMessage(String details) {
        return "⚠️ **Не удалось создать событие в Яндекс-Календаре:**\n\n" +
                (details != null && !details.isEmpty() ? details : CALDAV_NOT_CONFIGURED_OR_FAILED_MESSAGE) + "\n\n" +
                "Пожалуйста, проверьте подключение и токен доступа в окне Настроек (вкладка **«Яндекс 360»**).";
    }

    String BOOKING_VERBS_REGEX = "(?<![\\p{L}])(?:создай|сделай|запланируй|добавь|внеси|запиши|поставь|назначь|забронируй|организуй)(?![\\p{L}])";
    java.util.regex.Pattern BOOKING_VERBS_PATTERN = java.util.regex.Pattern.compile(
            BOOKING_VERBS_REGEX,
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE
    );

    static boolean containsBookingVerb(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        return BOOKING_VERBS_PATTERN.matcher(message.trim()).find();
    }

    /**
     * Проверка, содержит ли пользовательское сообщение намерение управления календарем/созвонами.
     */
    boolean isMeetingBookingIntent(String userMessage);

    /**
     * Проверка, содержит ли пользовательское сообщение намерение просмотра, поиска или проверки расписания календаря.
     */
    boolean isCalendarQueryIntent(String userMessage);

    /**
     * Формирование структурированного отчета по событиям Яндекс-Календаря пользователя за запрошенный период.
     */
    String getCalendarScheduleSummary(UUID currentUserId, String userMessage);

    /**
     * Интеллектуальный разбор намерения пользователя (определение личного или корпоративного календаря,
     * поиск кандидата по ФИО в базе HRM, вычисление времени с учетом часового пояса Europe/Saratov,
     * параметров видеовстречи Телемоста).
     */
    AiMeetingParseResult parseMeetingIntent(String userMessage, UUID currentUserId);

    /**
     * Выполнение бронирования встречи: вызов YandexIntegrationService, создание события CalDAV с инвайтами
     * кандидату и сотрудникам, генерация ссылки на Телемост и сохранение записи в IteractionList.
     */
    YandexMeetingResult executeMeetingBooking(UUID currentUserId, AiMeetingParseResult parsedIntent);
}
