package com.company.hunttech.service;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.service.dto.avatar.AvatarApplyMode;
import com.company.hunttech.service.dto.avatar.ResolvedAvatarInfo;
import com.haulmont.cuba.core.entity.FileDescriptor;

import java.util.UUID;

/**
 * Сервис управления и разрешения аватаров пользователей HRM HuntTech.
 * Реализует бизнес-логику приоритетов, кадрового fallback, защиты личного выбора сотрудника
 * и безопасной очистки файлового хранилища FileStorage.
 */
public interface UserAvatarManagementService {
    String NAME = "hunttech_UserAvatarManagementService";

    /**
     * Разрешает эффективный аватар пользователя с учетом приоритетов и проверки доступности в FileStorage:
     * 1. Личный аватар (ExtUser.userAvatar);
     * 2. Официальное фото (ExtUser.officialPhoto);
     * 3. Устаревшее фото (ExtUser.fileImageFace);
     * 4. Системная заглушка темы (icons/no-programmer.jpeg).
     *
     * @param user пользователь системы
     * @return DTO с эффективным дескриптором и типом источника
     */
    ResolvedAvatarInfo resolveEffectiveAvatar(ExtUser user);

    /**
     * Разрешает эффективный аватар по идентификатору пользователя.
     * Загружает пользователя с необходимыми атрибутами и разрешает аватар.
     *
     * @param userId UUID пользователя
     * @return DTO с эффективным дескриптором и типом источника
     */
    ResolvedAvatarInfo resolveEffectiveAvatar(UUID userId);

    /**
     * Сохранение персонального аватара пользователем (из ExtSettingsWindow).
     * Обновляет ExtUser.userAvatar, зеркалирует в UserSettings.fileImageFace и удаляет старый файл при отсутствии ссылок.
     *
     * @param user               пользователь
     * @param uploadedDescriptor новый загруженный FileDescriptor
     * @return обновленный экземпляр ExtUser
     */
    ExtUser applyUserPersonalAvatar(ExtUser user, FileDescriptor uploadedDescriptor);

    /**
     * Удаление персонального аватара пользователем (кнопка «Очистить» в ExtSettingsWindow).
     * Сбрасывает ExtUser.userAvatar и UserSettings.fileImageFace в null, производит безопасную очистку хранилища.
     *
     * @param user пользователь
     * @return обновленный экземпляр ExtUser
     */
    ExtUser clearUserPersonalAvatar(ExtUser user);

    /**
     * Сохранение корпоративного фото администратором (из ExtUserEditor / Telegram) с явным режимом применения.
     *
     * @param user               редактируемый пользователь
     * @param uploadedDescriptor новый загруженный FileDescriptor
     * @param mode               режим применения (OFFICIAL_ONLY, OVERWRITE_ALL, SMART_DEFAULT)
     * @return обновленный экземпляр ExtUser
     */
    ExtUser applyAdminOfficialPhoto(ExtUser user, FileDescriptor uploadedDescriptor, AvatarApplyMode mode);

    /**
     * Удаление официального фото администратором.
     *
     * @param user пользователь
     * @return обновленный экземпляр ExtUser
     */
    ExtUser clearAdminOfficialPhoto(ExtUser user);

    /**
     * Безопасное удаление неиспользуемого файла из FileStorage и базы данных.
     * Файл удаляется только в том случае, если его идентификатор не совпадает ни с одной из активных ссылок.
     *
     * @param candidateForDeletion файл, предлагаемый к удалению
     * @param activeReferences     список активных ссылок на дескрипторы
     */
    void cleanupUnreferencedFile(FileDescriptor candidateForDeletion, FileDescriptor... activeReferences);
}
