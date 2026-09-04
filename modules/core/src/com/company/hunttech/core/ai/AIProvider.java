package com.company.hunttech.core.ai;

import com.company.hunttech.service.AiStreamListener;

import java.util.Map;

/**
 * Единый контракт текстовой генерации для всех внешних AI-сервисов.
 *
 * <p>Бизнес-сервис не зависит от HTTP-протокола конкретного поставщика:
 * реализация сама преобразует общие параметры в формат своего API.</p>
 */
public interface AIProvider {

    /** Код должен совпадать со значением UserAiConfiguration.providerCode. */
    String getProviderCode();

    /**
     * Выполняет синхронный запрос к модели с персональным ключом пользователя.
     * Пустое имя модели означает, что провайдер должен применить свою модель
     * по умолчанию; дополнительные параметры передаются через options.
     */
    String generateText(String prompt, String systemContext, String apiKey, String modelName,
                        Map<String, Object> options);

    /**
     * Выполняет синхронный запрос к модели и возвращает результат с информацией о токенах и стоимости.
     */
    default AiProviderResponse executeTextWithTokens(String prompt, String systemContext, String apiKey,
                                                    String modelName, Map<String, Object> options) {
        String text = generateText(prompt, systemContext, apiKey, modelName, options);
        int promptTokens = prompt != null ? (prompt.length() + (systemContext != null ? systemContext.length() : 0)) / 4 : 0;
        int completionTokens = text != null ? text.length() / 4 : 0;
        return AiProviderResponse.ofText(text, promptTokens, completionTokens, promptTokens + completionTokens);
    }

    /** Provider-level streaming capability. Legacy adapters remain synchronous by default. */
    default boolean supportsStreaming() {
        return false;
    }

    /** Streams text deltas when the provider supports it; otherwise fails explicitly. */
    default AiProviderResponse executeTextStreaming(String prompt, String systemContext, String apiKey,
                                                    String modelName, Map<String, Object> options,
                                                    AiStreamListener listener) {
        throw new UnsupportedOperationException(
                "Провайдер «" + getProviderCode() + "» не поддерживает streaming.");
    }

    /** Interrupts an active request identified by the HRM requestId, if supported. */
    default void cancelRequest(String requestId) {
        // Synchronous/legacy adapters have no interruptible request registry.
    }

    /**
     * Выполняет редактирование изображения (capability IMAGE_GENERATION).
     *
     * <p>Провайдер получает исходное изображение и возвращает обработанный
     * растровый файл (например PNG с прозрачным фоном). Реализация по умолчанию
     * не поддерживает изображения — текстовые провайдеры не обязаны её переопределять.</p>
     *
     * @param prompt         инструкция на языке модели (уже собранный prompt-шаблон функции)
     * @param systemContext  системный промпт функции
     * @param apiKey         API-ключ из AiCredentialService/UserAiConfiguration
     * @param modelName      имя image-модели; пустое — модель провайдера по умолчанию
     * @param options        общие опции функции (temperature и др.)
     * @param sourceImage    исходное изображение
     * @param sourceMimeType MIME-тип исходного изображения (image/png, image/jpeg, ...)
     * @return обработанное изображение
     */
    default byte[] generateImage(String prompt, String systemContext, String apiKey, String modelName,
                                 Map<String, Object> options, byte[] sourceImage, String sourceMimeType) {
        throw new UnsupportedOperationException(
                "Провайдер «" + getProviderCode() + "» не поддерживает IMAGE_GENERATION.");
    }
}
