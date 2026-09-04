package com.company.hunttech.service;

import java.util.Map;

/**
 * Единая точка выполнения AI-функций HRM HuntTech.
 *
 * <p>Потребитель передаёт стабильный functionCode и бизнес-контекст; provider, model,
 * prompt, credential source и fallback выбираются централизованно на middleware.</p>
 *
 * <p><b>Контракт пользовательской нотификации</b> (полный текст —
 * {@code docs/architecture/HRM_HuntTech_AI_User_Notification_Contract.md}): каждый
 * успешный реальный AI-вызов возвращает {@link AiExecutionResult} с метаданными —
 * модель, провайдер и собственник использованного API ({@link AiCredentialOwner}:
 * корпоративное подключение администратора или личное подключение пользователя).
 * Экран, инициировавший операцию, обязан показать пользователю исчезающую
 * TRAY-нотификацию стандартными средствами CUBA (см. web-утилиту AiOperationNotifier),
 * в которой указано, какая модель что сделала и чей API использован.</p>
 */
public interface AiExecutionService {
    String NAME = "hunttech_AiExecutionService";

    /**
     * Выполняет AI-функцию с текстовой capability (TEXT_GENERATION и др.).
     *
     * @param functionCode стабильный код активной AI-функции
     * @param context      бизнес-контекст для prompt-шаблона функции
     * @return сгенерированный текст ({@link AiExecutionResult#getText()}) и метаданные
     *         выполнения (модель, провайдер, собственник API)
     */
    AiExecutionResult executeText(String functionCode, Map<String, Object> context);

    /** Executes text generation and emits provider deltas when available. */
    default AiExecutionResult executeTextStreaming(String functionCode, Map<String, Object> context,
                                                   AiStreamListener listener) {
        AiExecutionResult result = executeText(functionCode, context);
        if (listener != null && result != null && result.getText() != null) {
            listener.onDelta(result.getText());
        }
        return result;
    }

    /**
     * Выполняет AI-функцию с capability IMAGE_GENERATION над переданным изображением.
     *
     * @param functionCode    стабильный код активной AI-функции
     * @param context         бизнес-контекст для prompt-шаблона функции
     * @param sourceImage     исходное изображение (растровое)
     * @param sourceMimeType  MIME-тип исходного изображения (image/png, image/jpeg, ...)
     * @return обработанное изображение ({@link AiExecutionResult#getImage()}) и метаданные
     *         выполнения (модель, провайдер, собственник API)
     */
    AiExecutionResult executeImage(String functionCode, Map<String, Object> context,
                                   byte[] sourceImage, String sourceMimeType);
}
