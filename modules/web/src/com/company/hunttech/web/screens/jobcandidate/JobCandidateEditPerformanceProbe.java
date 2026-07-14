package com.company.hunttech.web.screens.jobcandidate;

import org.slf4j.Logger;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Собирает структурированные замеры открытия JobCandidateEdit.
 * По умолчанию отключён и не влияет на штатное поведение экрана.
 */
final class JobCandidateEditPerformanceProbe {

    static final String ENABLED_PROPERTY = "hrm.jobCandidateEdit.performance.enabled";
    static final String LOG_PREFIX = "JOB_CANDIDATE_EDIT_PERF";

    private final boolean enabled;
    private final LongSupplier nanoTime;
    private final Consumer<String> logSink;

    private String openId;
    private String candidateId = "not-loaded";
    private long screenStartNanos;
    private long lastMarkNanos;

    JobCandidateEditPerformanceProbe(Logger logger) {
        this(Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "false")),
                System::nanoTime,
                logger::info);
    }

    JobCandidateEditPerformanceProbe(boolean enabled,
                                     LongSupplier nanoTime,
                                     Consumer<String> logSink) {
        this.enabled = enabled;
        this.nanoTime = nanoTime;
        this.logSink = logSink;
    }

    boolean isEnabled() {
        return enabled;
    }

    void start(String candidateId) {
        if (!enabled) {
            return;
        }
        this.openId = UUID.randomUUID().toString();
        this.candidateId = sanitize(candidateId);
        this.screenStartNanos = nanoTime.getAsLong();
        this.lastMarkNanos = screenStartNanos;
        emit("screen.open.start", 0L, "OK", null);
    }

    void setCandidateId(String candidateId) {
        if (enabled) {
            this.candidateId = sanitize(candidateId);
        }
    }

    long begin() {
        return enabled ? nanoTime.getAsLong() : 0L;
    }

    void checkpoint(String phase) {
        if (!enabled) {
            return;
        }
        long now = nanoTime.getAsLong();
        emit(phase, now - lastMarkNanos, "OK", null);
        lastMarkNanos = now;
    }

    void record(String phase, long startedNanos) {
        if (!enabled) {
            return;
        }
        long now = nanoTime.getAsLong();
        emit(phase, now - startedNanos, "OK", null);
        lastMarkNanos = now;
    }

    void finish() {
        if (!enabled) {
            return;
        }
        long now = nanoTime.getAsLong();
        emit("screen.visible.total", now - screenStartNanos, "OK", null);
        lastMarkNanos = now;
    }

    void measure(String phase, Runnable action) {
        if (!enabled) {
            action.run();
            return;
        }
        long startedNanos = nanoTime.getAsLong();
        try {
            action.run();
            record(phase, startedNanos);
        } catch (RuntimeException | Error exception) {
            recordError(phase, startedNanos, exception);
            throw exception;
        }
    }

    <T> T measure(String phase, Supplier<T> action) {
        if (!enabled) {
            return action.get();
        }
        long startedNanos = nanoTime.getAsLong();
        try {
            T result = action.get();
            record(phase, startedNanos);
            return result;
        } catch (RuntimeException | Error exception) {
            recordError(phase, startedNanos, exception);
            throw exception;
        }
    }

    private void recordError(String phase, long startedNanos, Throwable exception) {
        long now = nanoTime.getAsLong();
        emit(phase, now - startedNanos, "ERROR", exception.getClass().getName());
        lastMarkNanos = now;
    }

    private void emit(String phase, long elapsedNanos, String status, String errorClass) {
        double elapsedMs = Math.max(0L, elapsedNanos) / 1_000_000.0d;
        StringBuilder line = new StringBuilder(192)
                .append(LOG_PREFIX)
                .append("|openId=").append(openId)
                .append("|candidateId=").append(candidateId)
                .append("|phase=").append(sanitize(phase))
                .append("|elapsedMs=").append(String.format(Locale.ROOT, "%.3f", elapsedMs))
                .append("|status=").append(status)
                .append("|thread=").append(sanitize(Thread.currentThread().getName()));
        if (errorClass != null) {
            line.append("|error=").append(sanitize(errorClass));
        }
        logSink.accept(line.toString());
    }

    private static String sanitize(String value) {
        return value == null ? "null" : value.replace('|', '_').replace('\n', '_').replace('\r', '_');
    }
}
