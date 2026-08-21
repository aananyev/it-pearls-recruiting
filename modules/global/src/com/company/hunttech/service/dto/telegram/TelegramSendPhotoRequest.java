package com.company.hunttech.service.dto.telegram;

import com.haulmont.cuba.core.entity.FileDescriptor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Запрос на отправку фотографии с подписью пользователю, в группу или канал.
 * <p>
 * Источники фото (взаимоисключающие, порядок приоритета: photoBytes > fileDescriptor > telegramFileId):
 * 1. {@link #photoBytes} — отправка сырых байтов из памяти.
 * 2. {@link #fileDescriptor} — отправка файла из CUBA FileStorage.
 * 3. {@link #telegramFileId} — пересылка уже существующего файла на серверах Telegram.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramSendPhotoRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Идентификатор чата (числовой chatId или username канала с @)
     */
    private String targetChatId;

    /**
     * Бинарные данные изображения (приоритет 1)
     */
    private byte[] photoBytes;

    /**
     * Имя файла изображения (например, "avatar.jpg" или "chart.png")
     */
    private String photoFileName;

    /**
     * CUBA FileDescriptor (приоритет 2, если photoBytes не заданы)
     */
    private FileDescriptor fileDescriptor;

    /**
     * Telegram file_id (приоритет 3, если photoBytes и fileDescriptor не заданы)
     */
    private String telegramFileId;

    /**
     * Подпись к фото (caption)
     */
    private String caption;

    /**
     * Режим форматирования подписи: "HTML", "Markdown", "MarkdownV2" или null
     */
    @Builder.Default
    private String parseMode = "HTML";

    /**
     * Отправить без звукового уведомления
     */
    private Boolean disableNotification;

    /**
     * ID сообщения для ответа
     */
    private Integer replyToMessageId;

    /**
     * Список рядов inline-кнопок
     */
    private List<List<TelegramSendMessageRequest.InlineButtonDto>> inlineKeyboard;
}
