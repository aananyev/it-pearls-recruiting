package com.company.hunttech.core.ai;

import org.springframework.stereotype.Component;

/** Подключение к моделям GLM китайской платформы Z.AI. */
@Component
public class GlmProvider extends AbstractOpenAiCompatibleProvider {
    @Override public String getProviderCode() { return "glm"; }
    @Override protected String getApiUrl() { return "https://api.z.ai/api/paas/v4/chat/completions"; }
    @Override protected String getDefaultModel() { return "glm-5.1"; }
    @Override protected String getProviderDisplayName() { return "Z.AI GLM"; }
}
