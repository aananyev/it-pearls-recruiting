package com.company.hunttech.core.ai;

import com.company.hunttech.ai.AiProviderCatalog;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Проверяет единый публичный каталог AI-провайдеров без сетевых запросов.
 *
 * Тест защищает Web Client от рассинхронизации providerCode/defaultModel с
 * фактическими core provider implementations.
 */
public class AIProviderCatalogTest {

    @Test
    public void globalCatalogMatchesCoreProviders() {
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
        assertEquals(modelsByProvider, AiProviderCatalog.getDefaultModels());
        assertEquals(modelsByProvider.keySet(),
                new java.util.LinkedHashSet<>(AiProviderCatalog.getProviderOptions().values()));
        assertEquals("deepseek-v4-flash", AiProviderCatalog.getDefaultModel("deepseek"));
        for (Map.Entry<String, String> entry : modelsByProvider.entrySet()) {
            assertNotNull("Для провайдера " + entry.getKey() + " не задана модель", entry.getValue());
        }
    }
}
