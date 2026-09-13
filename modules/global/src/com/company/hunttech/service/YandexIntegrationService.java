package com.company.hunttech.service;

import com.company.hunttech.dto.yandex.*;
import com.company.hunttech.entity.UserYandexConfiguration;

import java.util.List;
import java.util.UUID;

public interface YandexIntegrationService {
    String NAME = "hunttech_YandexIntegrationService";

    /**
     * Диагностика подключения к выбранному сервису Yandex (AUTH, CALENDAR, TELEMOST, WIKI).
     */
    YandexDiagnosticResult testConnection(UUID userId, String serviceType);

    /**
     * Поиск доступных календарей пользователя через CalDAV discovery (calendar-home-set + Depth: 1).
     */
    List<YandexCalendarInfoDto> discoverCalendars(UUID userId);

    /**
     * Создание видеовстречи в Яндекс Телемост через Telemost API.
     */
    YandexTelemostConferenceDto createTelemostConference(UUID userId, boolean autoRecord, boolean aiSummary, String accessLevel);

    /**
     * Создание события в Яндекс.Календаре (CalDAV PUT RFC 5545), рассылка инвайтов через CalDAV outbox
     * и read-back верификация.
     */
    YandexMeetingResult scheduleCalendarEvent(UUID userId, YandexMeetingRequest request);

    /**
     * Отмена / удаление события из календаря.
     */
    boolean cancelCalendarEvent(UUID userId, String calendarPath, String eventUid);

    /**
     * Получить или создать запись конфигурации Yandex для указанного пользователя.
     */
    UserYandexConfiguration getOrCreateConfiguration(UUID userId);

    /**
     * Сохранение конфигурации с прозрачным шифрованием токенов через AiSecretService.
     */
    UserYandexConfiguration saveConfiguration(UserYandexConfiguration config, String plainOauthToken, String plainRefreshToken);
}
