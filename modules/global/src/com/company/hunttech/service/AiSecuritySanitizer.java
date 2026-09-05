package com.company.hunttech.service;

import java.util.regex.Pattern;

/** Removes credential-like values before an error is shown, logged, or audited. */
public final class AiSecuritySanitizer {
    private static final int MAX_ERROR_LENGTH = 1000;
    private static final Pattern AUTH_VALUE = Pattern.compile(
            "(?i)\\b(Bearer|Api[- ]?Key|Basic)\\s+[^\\s,;\\\"']+");
    private static final Pattern SECRET_FIELD = Pattern.compile(
            "(?i)([\\\"']?(?:api[-_ ]?key|access[-_ ]?token|refresh[-_ ]?token|password|secret|authorization|credential)[\\\"']?\\s*[:=]\\s*[\\\"']?)[^\\\"'\\s,;}]+([\\\"']?)");
    private static final Pattern LONG_TOKEN = Pattern.compile(
            "(?<![A-Za-z0-9])[A-Za-z0-9_\\-]{32,}(?![A-Za-z0-9])");

    private AiSecuritySanitizer() {
    }

    public static String sanitizeError(Throwable failure) {
        return sanitizeError(failure == null ? null : failure.getMessage());
    }

    public static String sanitizeError(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }
        String sanitized = AUTH_VALUE.matcher(message).replaceAll("$1 [REDACTED]");
        sanitized = SECRET_FIELD.matcher(sanitized).replaceAll("$1[REDACTED]$2");
        sanitized = LONG_TOKEN.matcher(sanitized).replaceAll("[REDACTED]");
        sanitized = sanitized.trim();
        return sanitized.length() > MAX_ERROR_LENGTH
                ? sanitized.substring(0, MAX_ERROR_LENGTH)
                : sanitized;
    }
}
