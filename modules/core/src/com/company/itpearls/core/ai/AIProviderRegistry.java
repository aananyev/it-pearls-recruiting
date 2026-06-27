package com.company.itpearls.core.ai;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AIProviderRegistry {

    private final Map<String, AIProvider> providers;

    public AIProviderRegistry(List<AIProvider> providerList) {
        providers = new HashMap<>();
        if (providerList != null) {
            for (AIProvider provider : providerList) {
                providers.put(provider.getProviderCode(), provider);
            }
        }
    }

    public AIProvider getProvider(String code) {
        AIProvider provider = providers.get(code);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown AI provider code: " + code);
        }
        return provider;
    }
}
