package com.company.hunttech.service;

import com.company.hunttech.dto.yandex.*;
import com.company.hunttech.entity.UserYandexConfiguration;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface YandexIntegrationService {
    String NAME = "hunttech_YandexIntegrationService";
    String DEFAULT_PERSONAL_CALENDAR_NAME = "Основной календарь";

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
     * Получить список событий из указанного календаря за период времени.
     */
    List<YandexCalendarEventDto> getCalendarEvents(UUID userId, String calendarPath, Date from, Date to);

    /**
     * Получить список событий из всех настроенных календарей (личного и корпоративного) за период времени.
     */
    List<YandexCalendarEventDto> getAllUpcomingCalendarEvents(UUID userId, Date from, Date to);

    /**
     * Получить или создать запись конфигурации Yandex для указанного пользователя.
     */
    UserYandexConfiguration getOrCreateConfiguration(UUID userId);

    /**
     * Сохранение конфигурации с прозрачным шифрованием токенов через AiSecretService.
     */
    UserYandexConfiguration saveConfiguration(UserYandexConfiguration config, String plainOauthToken, String plainRefreshToken);

    /**
     * Шифрование OAuth токена через AiSecretService без сохранения в БД.
     *
     * @param plainOauthToken открытый OAuth токен (если null или пустой, возвращается null)
     * @return зашифрованная строка токена для безопасного сохранения в БД
     * @throws RuntimeException при возникновении ошибки алгоритма шифрования
     */
    String encryptOauthToken(String plainOauthToken);

    /**
     * Получить список всех доступных пользователю календарей (персональный + активные корпоративные)
     * с расчетом календаря по умолчанию по строгому приоритету:
     * 1) персональный календарь пользователя (если настроен и задан как default);
     * 2) корпоративный календарь по умолчанию (isDefault = true);
     * 3) первый доступный активный календарь;
     * 4) пустой список, если календарей нет.
     */
    List<YandexCalendarInfoDto> getAvailableCalendars(UUID userId);

    /**
     * Идемпотентная синхронизация события взаимодействия с кандидатом в Яндекс Календаре.
     * При addToCalendar=false — удаляет ранее созданное событие (если было).
     * При addToCalendar=true — создает или обновляет событие CalDAV с инвайтом кандидату и ссылкой на Телемост.
     * Не бросает исключение при сетевых/API сбоях, возвращая результат со статусом и предупреждением.
     */
    YandexMeetingResult syncInteractionCalendarEvent(UUID userId, UUID iteractionListId, boolean addToCalendar, String selectedCalendarPath, String userTimeZoneId);
}
