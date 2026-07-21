package com.company.hunttech.core.ai;

import org.springframework.stereotype.Component;

/** Подключение к моделям Grok компании xAI. */
@Component
public class GrokProvider extends AbstractOpenAiCompatibleProvider {
    @Override public String getProviderCode() { return "grok"; }
    @Override protected String getApiUrl() { return "https://api.x.ai/v1/chat/completions"; }
    @Override protected String getDefaultModel() { return "grok-4.3"; }
    @Override protected String getProviderDisplayName() { return "xAI Grok"; }
}
