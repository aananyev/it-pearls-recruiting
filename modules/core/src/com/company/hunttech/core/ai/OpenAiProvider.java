package com.company.hunttech.core.ai;

import org.springframework.stereotype.Component;

/** Подключение к OpenAI через Chat Completions API. */
@Component
public class OpenAiProvider extends AbstractOpenAiCompatibleProvider {
    @Override public String getProviderCode() { return "openai"; }
    @Override protected String getApiUrl() { return "https://api.openai.com/v1/chat/completions"; }
    @Override protected String getDefaultModel() { return "gpt-4o"; }
    @Override protected String getProviderDisplayName() { return "OpenAI"; }
}
