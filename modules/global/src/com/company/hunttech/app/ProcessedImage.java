package com.company.hunttech.app;

import com.company.hunttech.service.AiExecutionResult;

import java.io.Serializable;

/**
 * Результат обработки изображения конвейером {@link ProjectLogoImageProcessingService}.
 */
public class ProcessedImage implements Serializable {

    private final byte[] data;
    private final String name;
    private final String extension;
    private final boolean processed;

    /**
     * {@code true}, если фон изображения был удалён нейросетью (rembg/u2net или
     * AI-функция {@code PROJECT_LOGO_IMAGE_GENERATE}), а не классическим конвейером
     * (flood-fill). Используется для честного уведомления пользователя об AI-обработке
     * фотографии кандидата: конвертация в PNG + ресайз AI-обработкой не является.
     */
    private final boolean aiProcessed;

    /**
     * Метаданные AI-выполнения, если фон удалён платной AI-функцией
     * (модель, провайдер, собственник API — {@link AiExecutionResult}); {@code null},
     * если использован локальный rembg или классический конвейер.
     */
    private final AiExecutionResult aiExecution;

    public ProcessedImage(byte[] data, String name, String extension, boolean processed) {
        this(data, name, extension, processed, false, null);
    }

    public ProcessedImage(byte[] data, String name, String extension, boolean processed,
                          boolean aiProcessed) {
        this(data, name, extension, processed, aiProcessed, null);
    }

    public ProcessedImage(byte[] data, String name, String extension, boolean processed,
                          boolean aiProcessed, AiExecutionResult aiExecution) {
        this.data = data;
        this.name = name;
        this.extension = extension;
        this.processed = processed;
        this.aiProcessed = aiProcessed;
        this.aiExecution = aiExecution;
    }

    public byte[] getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public boolean isProcessed() {
        return processed;
    }

    public boolean isAiProcessed() {
        return aiProcessed;
    }

    /**
     * Метаданные платного AI-выполнения (модель, провайдер, собственник API) или
     * {@code null} при локальном rembg/классическом конвейере. Для нотификации
     * «обработано AI-функцией» с указанием собственника API проверяется именно он.
     */
    public AiExecutionResult getAiExecution() {
        return aiExecution;
    }
}
