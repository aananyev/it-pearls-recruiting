package com.company.hunttech.app;

/**
 * Безопасная нормализация пользовательских raster-изображений для sidebar.
 *
 * <p>Реальный codec определяется по содержимому через ImageIO, результат всегда PNG
 * не больше 512x512 без upscale. Контракт не удаляет фон, не crop-ит и не меняет
 * действующую модель хранения конкретного экрана.</p>
 */
public interface SidebarImageNormalizationService {

    String NAME = "hunttech_SidebarImageNormalizationService";

    int MAX_INPUT_BYTES = 20 * 1024 * 1024;

    ProcessedImage normalize(byte[] data, String fileName);
}
