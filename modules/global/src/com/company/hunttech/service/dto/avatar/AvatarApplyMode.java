package com.company.hunttech.service.dto.avatar;

/**
 * Режим применения загруженного администратором фото в карточке пользователя.
 */
public enum AvatarApplyMode {
    /**
     * Обновить только официальное корпоративное фото (ExtUser.officialPhoto), не изменяя личный аватар.
     */
    OFFICIAL_ONLY,

    /**
     * Принудительно обновить как официальное фото (ExtUser.officialPhoto),
     * так и личный аватар (ExtUser.userAvatar и UserSettings.fileImageFace).
     */
    OVERWRITE_ALL,

    /**
     * Умный режим по умолчанию: если у пользователя нет личного аватара — обновить оба слота;
     * если личный аватар уже установлен — сохранить только официальное фото (или запросить подтверждение в UI).
     */
    SMART_DEFAULT
}
