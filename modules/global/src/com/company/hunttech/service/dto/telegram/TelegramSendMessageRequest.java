package com.company.hunttech.service.dto.telegram;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Запрос на отправку текстового сообщения пользователю, в группу или канал.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramSendMessageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Идентификатор чата (числовой chatId или username канала с @, например "@my_channel")
     */
    private String targetChatId;

    /**
     * Текст сообщения
     */
    private String text;

    /**
     * Режим форматирования: "HTML", "Markdown", "MarkdownV2" или null (простой текст)
     */
    @Builder.Default
    private String parseMode = "HTML";

    /**
     * Отключить предпросмотр веб-ссылок
     */
    private Boolean disableWebPagePreview;

    /**
     * Отправить без звукового уведомления (silent mode)
     */
    private Boolean disableNotification;

    /**
     * ID сообщения, на которое создается ответ (reply_to_message_id)
     */
    private Integer replyToMessageId;

    /**
     * Список рядов inline-кнопок (опционально)
     */
    private List<List<InlineButtonDto>> inlineKeyboard;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InlineButtonDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String text;
        private String url;
        private String callbackData;
    }
}
