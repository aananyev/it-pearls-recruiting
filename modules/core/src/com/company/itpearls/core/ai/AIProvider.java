package com.company.itpearls.core.ai;

import java.util.Map;

public interface AIProvider {

    String getProviderCode();

    String generateText(String prompt, String systemContext, String apiKey, String modelName,
                        Map<String, Object> options);
}
