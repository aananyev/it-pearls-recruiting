package com.company.hunttech.service;

import com.company.hunttech.dto.yandex.AiMeetingParseResult;
import com.company.hunttech.dto.yandex.YandexMeetingResult;

import java.util.UUID;

public interface AiYandexOrchestrationService {
    String NAME = "hunttech_AiYandexOrchestrationService";

    /**
     * Быстрая проверка, содержит ли пользовательское сообщение намерение управления календарем/созвонами.
     */
    boolean isMeetingBookingIntent(String userMessage);

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
