package com.company.hunttech.core.ai;

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
}
