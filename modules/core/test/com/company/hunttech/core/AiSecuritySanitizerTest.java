package com.company.hunttech.core;

import com.company.hunttech.service.AiSecuritySanitizer;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AiSecuritySanitizerTest {
    @Test
    public void redactsAuthorizationAndSecretFields() {
        String raw = "Authorization: Bearer bearer-secret-marker "
                + "{\"apiKey\":\"json-secret-marker\"}";

        String sanitized = AiSecuritySanitizer.sanitizeError(raw);

        assertNotNull(sanitized);
        assertFalse(sanitized.contains("bearer-secret-marker"));
        assertFalse(sanitized.contains("json-secret-marker"));
        assertTrue(sanitized.contains("[REDACTED]"));
    }

    @Test
    public void emptyErrorRemainsEmptyForAuditField() {
        assertNull(AiSecuritySanitizer.sanitizeError((String) null));
        assertNull(AiSecuritySanitizer.sanitizeError("  "));
    }
}
