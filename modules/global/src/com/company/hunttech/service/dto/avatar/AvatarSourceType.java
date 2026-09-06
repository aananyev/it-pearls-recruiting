package com.company.hunttech.service.dto.avatar;

/**
 * Источники эффективного аватара пользователя в порядке приоритета.
 */
public enum AvatarSourceType {
    /**
     * Личный аватар сотрудника, загруженный им самостоятельно (высший приоритет).
     */
    USER_PERSONAL,

    /**
     * Официальное корпоративное фото, установленное администратором или импортированное из Telegram (кадровый fallback).
     */
    ADMIN_OFFICIAL,

    /**
     * Историческое фото из устаревшего поля fileImageFace (legacy fallback).
     */
    LEGACY_PHOTO,

    /**
     * Системная заглушка темы оформления по умолчанию (icons/no-programmer.jpeg).
     */
    THEME_DEFAULT
}
