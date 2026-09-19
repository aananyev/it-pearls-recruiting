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
                    new GlmProvider(),
                    new OpenRouterProvider(),
                    new BAIProvider());

            Map<String, String> modelsByProvider = new LinkedHashMap<>();
            for (AbstractOpenAiCompatibleProvider provider : providers) {
                modelsByProvider.put(provider.getProviderCode(), provider.getDefaultModel());
            }

            assertEquals("Каталог должен содержать ровно двенадцать уникальных кодов", 12,
                    modelsByProvider.size());
            assertEquals(modelsByProvider, AiProviderCatalog.getDefaultModels());
            assertEquals(modelsByProvider.keySet(),
                    new java.util.LinkedHashSet<>(AiProviderCatalog.getProviderOptions().values()));
            assertEquals("deepseek-v4-flash", AiProviderCatalog.getDefaultModel("deepseek"));
            assertEquals("nvidia/nemotron-3-ultra-550b-a55b:free", AiProviderCatalog.getDefaultModel("openrouter"));
            for (Map.Entry<String, String> entry : modelsByProvider.entrySet()) {
                assertNotNull("Для провайдера " + entry.getKey() + " не задана модель", entry.getValue());
            }
        }

    @Test
    public void openRouterProviderModelResolution() {
        OpenRouterProvider provider = new OpenRouterProvider();
        assertEquals("openrouter", provider.getProviderCode());
        assertEquals("nvidia/nemotron-3-ultra-550b-a55b:free", provider.getDefaultModel());
        assertEquals("nvidia/nemotron-3-ultra-550b-a55b:free", provider.resolveModelName(null));
        assertEquals("nvidia/nemotron-3-ultra-550b-a55b:free", provider.resolveModelName(""));
        assertEquals("nvidia/nemotron-3-ultra-550b-a55b:free", provider.resolveModelName("openrouter/openai"));
        assertEquals("nvidia/nemotron-3-ultra-550b-a55b:free", provider.resolveModelName("nemotron-3-ultra-550b"));
        assertEquals("nvidia/nemotron-3-ultra-550b-a55b:free", provider.resolveModelName("nemotron-3-ultra-550b:free"));
        assertEquals("nvidia/nemotron-3-ultra-550b-a55b:free", provider.resolveModelName("nvidia/nemotron-3-ultra-550b"));
        assertEquals("nvidia/nemotron-3-ultra-550b-a55b:free", provider.resolveModelName("nvidia/nemotron-3-ultra-550b:free"));
        assertEquals("nvidia/nemotron-3-ultra-550b-a55b:free", provider.resolveModelName("nemotron-3-ultra-550b-a55b:free"));
        assertEquals("nvidia/nemotron-3-ultra-550b-a55b", provider.resolveModelName("nemotron-3-ultra-550b-a55b"));
        assertEquals("nvidia/nemotron-3-ultra-550b-a55b", provider.resolveModelName("nvidia/nemotron-3-ultra-550b-a55b"));
        assertEquals("anthropic/claude-3.5-sonnet", provider.resolveModelName("anthropic/claude-3.5-sonnet"));
    }

    @Test
    public void baiProviderModelResolution() {
        BAIProvider provider = new BAIProvider();
        assertEquals("bai", provider.getProviderCode());
        assertEquals("glm-5.2", provider.getDefaultModel());
        assertEquals("glm-5.2", provider.resolveModelName(null));
        assertEquals("glm-5.2", provider.resolveModelName(""));
        assertEquals("glm-5.2", provider.resolveModelName("glm52"));
        assertEquals("glm-5.2", provider.resolveModelName("glm-52"));
        assertEquals("glm-5.2", provider.resolveModelName("glm5.2"));
        assertEquals("qwen3.8-flash", provider.resolveModelName("qwen"));
        assertEquals("qwen3.8-flash", provider.resolveModelName("qwen3"));
        assertEquals("qwen3.8-flash", provider.resolveModelName("qwen3.8"));
        assertEquals("custom-model", provider.resolveModelName("custom-model"));
    }
}

