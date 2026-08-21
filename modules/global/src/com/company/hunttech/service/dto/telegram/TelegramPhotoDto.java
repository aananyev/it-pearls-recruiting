package com.company.hunttech.service.dto.telegram;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO фотографии пользователя Telegram с метаданными и путем на сервере Telegram.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramPhotoDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Идентификатор файла в Telegram API (file_id)
     */
    private String fileId;

    /**
     * Уникальный глобальный идентификатор файла в Telegram (file_unique_id)
     */
    private String fileUniqueId;

    /**
     * Ширина изображения в пикселях
     */
    private Integer width;

    /**
     * Высота изображения в пикселях
     */
    private Integer height;

    /**
     * Размер файла в байтах
     */
    private Integer fileSize;

    /**
     * Путь к файлу на серверах Telegram (file_path)
     */
    private String filePath;

    /**
     * Фактическое разрешение фото
     */
    private PhotoResolution resolution;
}
