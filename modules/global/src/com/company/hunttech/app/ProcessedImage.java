package com.company.hunttech.app;

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

    public ProcessedImage(byte[] data, String name, String extension, boolean processed) {
        this(data, name, extension, processed, false);
    }

    public ProcessedImage(byte[] data, String name, String extension, boolean processed,
                          boolean aiProcessed) {
        this.data = data;
        this.name = name;
        this.extension = extension;
        this.processed = processed;
        this.aiProcessed = aiProcessed;
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
}
