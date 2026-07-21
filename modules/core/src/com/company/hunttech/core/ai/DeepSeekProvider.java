package com.company.hunttech.core.ai;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Подключение к китайскому сервису DeepSeek через совместимый Chat Completions API. */
@Component
public class DeepSeekProvider extends AbstractOpenAiCompatibleProvider {
    @Override public String getProviderCode() { return "deepseek"; }
    @Override protected String getApiUrl() { return "https://api.deepseek.com/chat/completions"; }
    @Override protected String getDefaultModel() { return "deepseek-v4-flash"; }
    @Override protected String getProviderDisplayName() { return "DeepSeek"; }

    @Override
    protected void customizeRequestBody(ObjectNode requestBody, Map<String, Object> options) {
        /* Для коротких HR-запросов отключаем вывод рассуждений: тест становится быстрее и дешевле. */
        requestBody.putObject("thinking").put("type", "disabled");
    }
}
