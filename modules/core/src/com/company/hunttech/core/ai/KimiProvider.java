package com.company.hunttech.core.ai;

import org.springframework.stereotype.Component;

/** Подключение к Kimi компании Moonshot AI. */
@Component
public class KimiProvider extends AbstractOpenAiCompatibleProvider {
    @Override public String getProviderCode() { return "kimi"; }
    @Override protected String getApiUrl() { return "https://api.moonshot.ai/v1/chat/completions"; }
    @Override protected String getDefaultModel() { return "kimi-k2.5"; }
    @Override protected String getProviderDisplayName() { return "Kimi"; }
}
