package com.company.hunttech.service.dto.telegram;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * Результат отправки сообщения или медиа в Telegram.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramSendResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Флаг успешности отправки
     */
    private boolean success;

    /**
     * Идентификатор отправленного сообщения в Telegram (message_id)
     */
    private Integer messageId;

    /**
     * Идентификатор чата, куда было отправлено сообщение
     */
    private Long chatId;

    /**
     * Текстовая причина ошибки (failure reason)
     */
    private String failureReason;

    /**
     * Код ошибки Telegram API (например, 400, 403, 404, 429)
     */
    private Integer errorCode;

    /**
     * Время отправки
     */
    @Builder.Default
    private Date timestamp = new Date();

    public static TelegramSendResult ok(Integer messageId, Long chatId) {
        return TelegramSendResult.builder()
                .success(true)
                .messageId(messageId)
                .chatId(chatId)
                .timestamp(new Date())
                .build();
    }

    public static TelegramSendResult fail(String failureReason) {
        return TelegramSendResult.builder()
                .success(false)
                .failureReason(failureReason)
                .timestamp(new Date())
                .build();
    }

    public static TelegramSendResult fail(Integer errorCode, String failureReason) {
        return TelegramSendResult.builder()
                .success(false)
                .errorCode(errorCode)
                .failureReason(failureReason)
                .timestamp(new Date())
                .build();
    }
}
