package com.company.hunttech.service;

import com.company.hunttech.service.dto.telegram.*;
import com.haulmont.cuba.core.entity.FileDescriptor;

/**
 * Сервис интеграции с Telegram Bot API:
 * - Получение профилей пользователей, метаданных и информации о чатах/каналах
 * - Получение и загрузка фотографий профиля по Telegram User ID / username в память и FileStorage
 * - Отправка текстовых сообщений, медиа и уведомлений пользователям, в группы и каналы
 */
public interface TelegramIntegrationService {
    String NAME = "hunttech_TelegramIntegrationService";

    /**
     * Проверка доступности конфигурации Telegram (задан ли токен и разрешен ли бот).
     *
     * @return true, если сервис готов к выполнению запросов
     */
    boolean isConfigured();

    /**
     * Получение информации о профиле пользователя по его числовому Telegram User ID.
     *
     * @param telegramUserId числовой ID пользователя Telegram
     * @return DTO профиля пользователя или null, если не найден / ошибка
     */
    TelegramUserProfileDto getUserProfile(Long telegramUserId);

    /**
     * Получение информации о профиле пользователя по строковому ID или username (e.g. "@username" или "123456789").
     *
     * @param telegramIdOrUsername строковый Telegram ID или username
     * @return DTO профиля пользователя или null
     */
    TelegramUserProfileDto getUserProfile(String telegramIdOrUsername);

    /**
     * Получение метаданных и информации о главной фотографии профиля пользователя.
     *
     * @param telegramUserId числовой ID пользователя Telegram
     * @param resolution желаемый размер/разрешение фотографии
     * @return DTO фотографии или null, если у пользователя нет фото
     */
    TelegramPhotoDto getUserProfilePhoto(Long telegramUserId, PhotoResolution resolution);

    /**
     * Получение метаданных фотографии профиля по строковому ID или username.
     *
     * @param telegramIdOrUsername строковый Telegram ID или username
     * @param resolution желаемый размер/разрешение фотографии
     * @return DTO фотографии или null
     */
    TelegramPhotoDto getUserProfilePhoto(String telegramIdOrUsername, PhotoResolution resolution);

    /**
     * Скачивание бинарного содержимого (байтов) фотографии профиля пользователя.
     *
     * @param telegramUserId числовой ID пользователя Telegram
     * @param resolution желаемый размер/разрешение фотографии
     * @return массив байтов изображения (обычно JPEG) или null, если фото отсутствует
     */
    byte[] downloadUserProfilePhotoBytes(Long telegramUserId, PhotoResolution resolution);

    /**
     * Скачивание бинарного содержимого фотографии профиля по строковому ID или username.
     *
     * @param telegramIdOrUsername строковый Telegram ID или username
     * @param resolution желаемый размер/разрешение фотографии
     * @return массив байтов изображения или null
     */
    byte[] downloadUserProfilePhotoBytes(String telegramIdOrUsername, PhotoResolution resolution);

    /**
     * Скачивание фотографии профиля пользователя и регистрация ее в CUBA FileStorage в виде FileDescriptor.
     *
     * @param telegramUserId числовой ID пользователя Telegram
     * @param customFileName пользовательское имя файла (например, "avatar_12345.jpg") или null для автогенерации
     * @return созданный и сохраненный в FileStorage FileDescriptor, либо null при ошибке/отсутствии фото
     */
    FileDescriptor saveUserProfilePhotoToFileStorage(Long telegramUserId, String customFileName);

    /**
     * Скачивание фотографии профиля по строковому ID или username и сохранение в CUBA FileStorage.
     *
     * @param telegramIdOrUsername строковый Telegram ID или username
     * @param customFileName пользовательское имя файла или null для автогенерации
     * @return созданный FileDescriptor или null
     */
    FileDescriptor saveUserProfilePhotoToFileStorage(String telegramIdOrUsername, String customFileName);

    /**
     * Получение информации о чате, канале или группе по числовому ID или username (например, "@my_channel").
     *
     * @param chatIdOrUsername ID чата (строка) или публичный username с '@'
     * @return DTO информации о чате
     */
    TelegramChatInfoDto getChatInfo(String chatIdOrUsername);

    /**
     * Отправка форматированного текстового сообщения (direct пользователю, в группу или канал).
     *
     * @param request параметры отправки сообщения
     * @return результат отправки с messageId или причиной ошибки
     */
    TelegramSendResult sendMessage(TelegramSendMessageRequest request);

    /**
     * Упрощенная отправка текстового сообщения.
     *
     * @param targetChatId ID чата / username канала
     * @param text текст сообщения (HTML)
     * @return результат отправки
     */
    TelegramSendResult sendMessage(String targetChatId, String text);

    /**
     * Отправка фотографии с подписью.
     *
     * @param request параметры отправки фото
     * @return результат отправки
     */
    TelegramSendResult sendPhoto(TelegramSendPhotoRequest request);
}
