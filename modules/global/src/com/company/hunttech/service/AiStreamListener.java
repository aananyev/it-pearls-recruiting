package com.company.hunttech.service;

/** Receives already decoded text deltas from a middleware streaming execution. */
@FunctionalInterface
public interface AiStreamListener {
    void onDelta(String text);
}
