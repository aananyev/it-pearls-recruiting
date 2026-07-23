package com.company.hunttech.core.ai;

import org.springframework.stereotype.Component;

/** Подключение к Qwen через международный endpoint Alibaba Cloud Model Studio. */
@Component
public class QwenProvider extends AbstractOpenAiCompatibleProvider {
    @Override public String getProviderCode() { return "qwen"; }
    @Override protected String getApiUrl() { return "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions"; }
    @Override protected String getDefaultModel() { return "qwen-plus"; }
    @Override protected String getProviderDisplayName() { return "Alibaba Qwen"; }
}
