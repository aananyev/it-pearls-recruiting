package com.company.hunttech.service.dto.telegram;

import java.io.Serializable;

/**
 * Разрешение/размер фотографии пользователя Telegram для загрузки.
 */
public enum PhotoResolution implements Serializable {
    /**
     * Превью / Маленькая иконка (обычно до 160x160)
     */
    THUMBNAIL,

    /**
     * Средний размер (обычно около 320x320)
     */
    MEDIUM,

    /**
     * Высокое разрешение (обычно 640x640)
     */
    HIGH_RESOLUTION,

    /**
     * Максимально доступное разрешение из предоставленных Telegram
     */
    LARGEST_AVAILABLE
}
