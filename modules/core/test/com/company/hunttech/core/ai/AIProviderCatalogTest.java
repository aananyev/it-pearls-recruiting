package com.company.hunttech.core.ai;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Проверяет публичный каталог AI-провайдеров без выполнения платных сетевых
 * запросов. Тест защищает SettingWindow от дублирования кодов и случайного
 * удаления модели по умолчанию при последующих изменениях интеграции.
 */
public class AIProviderCatalogTest {

    @Test
    public void catalogContainsTenUniqueProvidersWithDefaultModels() {
        List<AbstractOpenAiCompatibleProvider> providers = Arrays.asList(
                new YandexGptProvider(),
                new GigaChatProvider(),
                new OpenAiProvider(),
                new AnthropicProvider(),
                new GeminiProvider(),
                new GrokProvider(),
                new DeepSeekProvider(),
                new QwenProvider(),
                new KimiProvider(),
                new GlmProvider());

        Map<String, String> modelsByProvider = new LinkedHashMap<>();
        for (AbstractOpenAiCompatibleProvider provider : providers) {
            modelsByProvider.put(provider.getProviderCode(), provider.getDefaultModel());
        }

        assertEquals("Каталог должен содержать ровно десять уникальных кодов", 10,
                modelsByProvider.size());
        assertEquals("deepseek-v4-flash", modelsByProvider.get("deepseek"));
        assertEquals("yandexgpt/latest", modelsByProvider.get("yandex"));
        assertEquals("GigaChat", modelsByProvider.get("gigachat"));
        for (Map.Entry<String, String> entry : modelsByProvider.entrySet()) {
            assertNotNull("Для провайдера " + entry.getKey() + " не задана модель", entry.getValue());
        }
    }
}
