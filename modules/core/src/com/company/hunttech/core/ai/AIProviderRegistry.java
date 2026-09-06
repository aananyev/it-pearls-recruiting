package com.company.hunttech.core.ai;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AIProviderRegistry {

    private final Map<String, AIProvider> providers;
    private final ConcurrentMap<String, AIProvider> activeRequests = new ConcurrentHashMap<>();
    private final Set<String> pendingCancellations = ConcurrentHashMap.newKeySet();

    public AIProviderRegistry(List<AIProvider> providerList) {
        providers = new HashMap<>();
        if (providerList != null) {
            for (AIProvider provider : providerList) {
                /*
                 * Spring передаёт все компоненты AIProvider. Регистрация по
                 * стабильному коду позволяет добавлять новые сервисы без
                 * изменения HrmAiServiceBean и его бизнес-логики.
                 */
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

    /** Registers the selected adapter before opening its HTTP request. */
    public void registerRequest(String requestId, AIProvider provider) {
        if (requestId == null || requestId.trim().isEmpty() || provider == null) {
            return;
        }
        String normalized = requestId.trim();
        activeRequests.put(normalized, provider);
        if (pendingCancellations.remove(normalized)) {
            provider.cancelRequest(normalized);
        }
    }

    /** Removes the routing entry after the adapter has finished or failed. */
    public void unregisterRequest(String requestId, AIProvider provider) {
        if (requestId == null || provider == null) {
            return;
        }
        activeRequests.remove(requestId.trim(), provider);
        pendingCancellations.remove(requestId.trim());
    }

    /** Routes cancellation only to the adapter owning the active request. */
    public void cancelRequest(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return;
        }
        String normalized = requestId.trim();
        AIProvider provider = activeRequests.get(normalized);
        if (provider != null) {
            provider.cancelRequest(normalized);
        } else {
            pendingCancellations.add(normalized);
        }
    }
}
