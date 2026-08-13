package com.company.hunttech.service;

import java.util.Map;

/**
 * Единая точка выполнения AI-функций HRM HuntTech.
 *
 * Потребитель передаёт стабильный functionCode и бизнес-контекст; provider, model,
 * prompt, credential source и fallback выбираются централизованно на middleware.
 */
public interface AiExecutionService {
    String NAME = "hunttech_AiExecutionService";

    String executeText(String functionCode, Map<String, Object> context);

    /**
     * Выполняет AI-функцию с capability IMAGE_GENERATION над переданным изображением.
     *
     * @param functionCode    стабильный код активной AI-функции
     * @param context         бизнес-контекст для prompt-шаблона функции
     * @param sourceImage     исходное изображение (растровое)
     * @param sourceMimeType  MIME-тип исходного изображения (image/png, image/jpeg, ...)
     * @return обработанное изображение
     */
    byte[] executeImage(String functionCode, Map<String, Object> context,
                        byte[] sourceImage, String sourceMimeType);
}
