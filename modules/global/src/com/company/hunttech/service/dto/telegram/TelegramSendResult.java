package com.company.hunttech.service.dto.telegram;

import java.io.Serializable;
import java.util.Date;

/**
 * Результат отправки сообщения или медиа в Telegram.
 */
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
    private Date timestamp = new Date();

    public TelegramSendResult() {
    }

    public TelegramSendResult(boolean success, Integer messageId, Long chatId, String failureReason, Integer errorCode, Date timestamp) {
        this.success = success;
        this.messageId = messageId;
        this.chatId = chatId;
        this.failureReason = failureReason;
        this.errorCode = errorCode;
        this.timestamp = timestamp != null ? timestamp : new Date();
    }

    public static TelegramSendResult ok(Integer messageId, Long chatId) {
        TelegramSendResult res = new TelegramSendResult();
        res.setSuccess(true);
        res.setMessageId(messageId);
        res.setChatId(chatId);
        res.setTimestamp(new Date());
        return res;
    }

    public static TelegramSendResult fail(String failureReason) {
        TelegramSendResult res = new TelegramSendResult();
        res.setSuccess(false);
        res.setFailureReason(failureReason);
        res.setTimestamp(new Date());
        return res;
    }

    public static TelegramSendResult fail(Integer errorCode, String failureReason) {
        TelegramSendResult res = new TelegramSendResult();
        res.setSuccess(false);
        res.setErrorCode(errorCode);
        res.setFailureReason(failureReason);
        res.setTimestamp(new Date());
        return res;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
