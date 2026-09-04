package com.company.hunttech.service;

/** Receives already decoded text deltas from a middleware streaming execution. */
@FunctionalInterface
public interface AiStreamListener {
    void onDelta(String text);

    /** Provider-native request identifier, when it is known before completion. */
    default void onProviderRequestId(String providerRequestId) {
    }

    /** Usage reported by the provider, including a final usage event before an error. */
    default void onUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
    }
}
