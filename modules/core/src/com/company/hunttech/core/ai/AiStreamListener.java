package com.company.hunttech.core.ai;

/** Receives already decoded text deltas from a provider SSE stream. */
@FunctionalInterface
public interface AiStreamListener {
    void onDelta(String text);
}
