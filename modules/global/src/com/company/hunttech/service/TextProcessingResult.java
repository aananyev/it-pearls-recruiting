package com.company.hunttech.service;

import java.io.Serializable;

/**
 * Результат обработки текста сервисом {@link TextProcessingService}.
 * Содержит отформатированный текст и метаданные выполнения AI (модель, провайдер,
 * собственник API) в соответствии с HRM_HuntTech_AI_User_Notification_Contract.
 */
public class TextProcessingResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String text;
    private final AiExecutionResult aiExecution;

    public TextProcessingResult(String text, AiExecutionResult aiExecution) {
        this.text = text != null ? text : "";
        this.aiExecution = aiExecution;
    }

    public static TextProcessingResult aiResult(String text, AiExecutionResult aiExecution) {
        return new TextProcessingResult(text, aiExecution);
    }

    public static TextProcessingResult localResult(String text) {
        return new TextProcessingResult(text, null);
    }

    public String getText() {
        return text;
    }

    public AiExecutionResult getAiExecution() {
        return aiExecution;
    }

    public boolean isAiFormatted() {
        return aiExecution != null;
    }
}
